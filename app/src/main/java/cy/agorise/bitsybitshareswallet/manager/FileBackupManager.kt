package cy.agorise.bitsybitshareswallet.manager

import android.os.Environment
import com.google.common.primitives.Bytes
import com.google.gson.GsonBuilder
import cy.agorise.bitsybitshareswallet.dao.BitsyDatabase
import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import cy.agorise.bitsybitshareswallet.network.CryptoNetManager
import cy.agorise.bitsybitshareswallet.requestmanagers.*
import cy.agorise.graphenej.BrainKey
import cy.agorise.graphenej.FileBin
import cy.agorise.graphenej.Util
import cy.agorise.graphenej.models.backup.LinkedAccount
import cy.agorise.graphenej.models.backup.PrivateKeyBackup
import cy.agorise.graphenej.models.backup.Wallet
import cy.agorise.graphenej.models.backup.WalletBackup
import org.bitcoinj.core.ECKey
import org.tukaani.xz.CorruptedInputException
import org.tukaani.xz.LZMAInputStream
import org.tukaani.xz.XZInputStream
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.experimental.and

class FileBackupManager : FileServiceRequestsListener {

    override fun onNewRequest(request: FileServiceRequest) {
        if (request is CreateBackupRequest) {
            createBackupBinFile(request as CreateBackupRequest)
        } else if (request is ImportBackupRequest) {
            readBinFile(request as ImportBackupRequest)
        }
    }

    private fun createBackupBinFile(request: CreateBackupRequest) {
        val db = BitsyDatabase.getAppDatabase(request.context)
        val seedNames = ArrayList<BitsharesSeedName>()
        val seeds = db!!.accountSeedDao().allNoLiveData
        for (seed in seeds) {
            val accounts = db!!.cryptoNetAccountDao().getAllCryptoNetAccountBySeed(seed.id)
            for (account in accounts) {
                if (account.cryptoNet!!.equals(CryptoNet.BITSHARES)) {
                    seedNames.add(BitsharesSeedName(account.name!!, seed.masterSeed!!))
                }
            }
        }

        getBinBytesFromBrainkey(seedNames, request) //TODO make funcion for non-bitshares accounts
    }

    private fun getBinBytesFromBrainkey(bitsharesSeedNames: List<BitsharesSeedName>, request: CreateBackupRequest) {

        try {
            val wallets = ArrayList<Wallet>()
            val accounts = ArrayList<LinkedAccount>()
            val keys = ArrayList<PrivateKeyBackup>()
            var fileName: String? =
                null //TODO choice a good name,  now we use the first bitshares account as the bin backup
            for (bitsharesSeedName in bitsharesSeedNames) {
                if (fileName == null) {
                    fileName = bitsharesSeedName.accountName
                }

                val sequence = 0
                //TODO adapt CHAIN ID
                val wallet = Wallet(
                    bitsharesSeedName.accountName,
                    bitsharesSeedName.accountSeed,
                    sequence,
                    CryptoNetManager.getChaindId(CryptoNet.BITSHARES),
                    request.password
                )
                wallets.add(wallet)

                val brainKey = BrainKey(bitsharesSeedName.accountSeed, sequence) //TODO chain to use BIP39
                val keyBackup = PrivateKeyBackup(
                    brainKey.privateKey.privKeyBytes,
                    sequence, sequence, wallet.getEncryptionKey(request.password)
                )
                keys.add(keyBackup)
                val linkedAccount =
                    LinkedAccount(bitsharesSeedName.accountName, CryptoNetManager.getChaindId(CryptoNet.BITSHARES))
                accounts.add(linkedAccount)
            }

            val backup = WalletBackup(wallets, keys, accounts)
            val results = FileBin.serializeWalletBackup(backup, request.password)
            val resultFile = ArrayList<Int>()
            for (result in results!!) {
                resultFile.add((result and 0xff.toByte()).toInt())
            }
            saveBinContentToFile(resultFile, fileName, request)
        } catch (e: Exception) {
            e.printStackTrace()
            request.setStatus(CreateBackupRequest.StatusCode.FAILED)
            //TODO error exception

        }

    }

    private fun saveBinContentToFile(content: List<Int>, fileName: String?, request: CreateBackupRequest) {
        val df = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
        val dateHourString = df.format(Date())

        val folder =
            Environment.getExternalStorageDirectory().toString() + File.separator + "Crystal" //TODO make constant
        val path = folder + File.separator + fileName + dateHourString + ".bin"

        val folderFile = File(folder)
        if (!folderFile.exists()) {
            if (folderFile.mkdirs()) {
                println("folder created")
            } else {
                println("couldn't create folder")
                request.setStatus(CreateBackupRequest.StatusCode.FAILED)
                return
            }
        }

        val success = saveBinFile(path, content, request)
        if (success) {
            request.setFilePath(path)
            request.setStatus(CreateBackupRequest.StatusCode.SUCCEEDED)
        } else {
            request.setStatus(CreateBackupRequest.StatusCode.FAILED)
        }

    }

    private fun readBinFile(request: ImportBackupRequest) {
        try {
            val file = File(request.filePath)
            val dis = DataInputStream(FileInputStream(file))

            val readBytes = ArrayList<Int>()


            for (i in 0 until file.length()) {
                val `val` = unsignedToBytes(dis.readByte())
                readBytes.add(`val`)
            }


            dis.close()
            val byteArray = ByteArray(readBytes.size)
            for (i in readBytes.indices) {
                byteArray[i] = readBytes[i].toByte()
            }

            val walletBackup = deserializeWalletBackup(byteArray, request.password)
            if (walletBackup == null) {
                //TODO handle error
                println("FileBackupManager error walletBackup null")
                request.setStatus(ImportBackupRequest.StatusCode.FAILED)
                return
            }

            val seedNames = ArrayList<BitsharesSeedName>()

            for (i in 0 until walletBackup.keyCount) {
                val brainKey = walletBackup.getWallet(i).decryptBrainKey(request.password)
                val sequence = walletBackup.getWallet(i).brainkeySequence
                val accountName = walletBackup.getWallet(i).privateName
                seedNames.add(BitsharesSeedName(accountName, brainKey))
            }
            //TODO handle more than one account
            val db = BitsyDatabase.getAppDatabase(request.context)
            val accountSeedDao = db!!.accountSeedDao()
            for (seedName in seedNames) {
                val validatorRequest = ValidateImportBitsharesAccountRequest(
                    seedName.accountName,
                    seedName.accountSeed,
                    request.context,
                    true
                )
                validatorRequest.listener = object : CryptoNetInfoRequestListener {
                    override fun onCarryOut() {
                        if (!validatorRequest.status.equals(ValidateImportBitsharesAccountRequest.StatusCode.SUCCEEDED)) {
                            request.setStatus(ImportBackupRequest.StatusCode.FAILED) // TODO reason bad seed
                        } else {
                            /*AccountSeed seed = new AccountSeed();
                            seed.setName(validatorRequest.getAccountName());
                            seed.setType(validatorRequest.getSeedType());
                            seed.setMasterSeed(validatorRequest.getMnemonic());
                            long idSeed = accountSeedDao.insertAccountSeed(seed);
                            if(idSeed >= 0) {
                                GrapheneAccount account = new GrapheneAccount();
                                account.setCryptoNet(CryptoNet.BITSHARES);
                                account.setAccountIndex(0);
                                account.setSeedId(idSeed);
                                account.setName(validatorRequest.getAccountName());
                                BitsharesAccountManager bManger = new BitsharesAccountManager();
                                bManger.importAccountFromSeed(account,request.context);
                                request.setStatus(ImportBackupRequest.StatusCode.SUCCEEDED);
                            }else{
                                request.setStatus(ImportBackupRequest.StatusCode.FAILED); //TODO reason couldn't insert seed
                            }*/
                            request.setStatus(ImportBackupRequest.StatusCode.SUCCEEDED)
                        }
                    }
                }
                CryptoNetInfoRequests.instance_!!.addRequest(validatorRequest)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            request.setStatus(ImportBackupRequest.StatusCode.FAILED)
            //TODO handle exception
        }

    }

    private fun unsignedToBytes(b: Byte): Int {
        return (b and 0xFF.toByte()).toInt()
    }

    inner class BitsharesSeedName(internal var accountName: String, internal var accountSeed: String)

    companion object {

        private fun saveBinFile(filePath: String, content: List<Int>, request: CreateBackupRequest): Boolean {
            var success = false
            try {
                //TODO permissions
                // PermissionManager Manager = new PermissionManager();
                // Manager.verifyStoragePermissions(_activity);

                val file = File(filePath)
                val fileData = ByteArray(content.size)

                val bos = BufferedOutputStream(FileOutputStream(file))

                for (i in content.indices) {
                    fileData[i] = content[i].toByte()
                }

                bos.write(fileData)
                bos.flush()
                bos.close()

                success = true
            } catch (e: Exception) {
                e.printStackTrace()
                //TODO handle error
            }

            return success
        }


        /**
         * This part is copied from the graphenej library, to edit possible error.
         * TODO in graphenej library to catch EOFException reading files
         */

        fun deserializeWalletBackup(input: ByteArray, password: String): WalletBackup? {
            try {
                val publicKey = ByteArray(33)
                val rawDataEncripted = ByteArray(input.size - 33)

                System.arraycopy(input, 0, publicKey, 0, 33)
                System.arraycopy(input, 33, rawDataEncripted, 0, rawDataEncripted.size)

                val md = MessageDigest.getInstance("SHA-256")

                val randomECKey = ECKey.fromPublicOnly(publicKey)
                var finalKey =
                    randomECKey.pubKeyPoint.multiply(ECKey.fromPrivate(md.digest(password.toByteArray(charset("UTF-8")))).privKey)
                        .normalize().xCoord.encoded
                val md1 = MessageDigest.getInstance("SHA-512")
                finalKey = md1.digest(finalKey)
                val decryptedData = Util.decryptAES(rawDataEncripted, Util.bytesToHex(finalKey).toByteArray())

                val checksum = ByteArray(4)
                System.arraycopy(decryptedData!!, 0, checksum, 0, 4)
                val compressedData = ByteArray(decryptedData.size - 4)
                System.arraycopy(decryptedData, 4, compressedData, 0, compressedData.size)

                val decompressedData = decompress(compressedData, Util.LZMA)
                val walletString = String(decompressedData!!, Charsets.UTF_8)
                println("Wallet str: $walletString")
                return GsonBuilder().create().fromJson(walletString, WalletBackup::class.java)
            } catch (e: NoSuchAlgorithmException) {
                println("NoSuchAlgorithmException. Msg: " + e.message)
            } catch (e: UnsupportedEncodingException) {
                println("UnsupportedEncodingException. Msg: " + e.message)
            }

            return null
        }

        fun decompress(inputBytes: ByteArray, which: Int): ByteArray? {
            var `in`: InputStream? = null
            try {
                println("Bytes: " + Util.bytesToHex(inputBytes))
                var input = ByteArrayInputStream(inputBytes)
                var output = ByteArrayOutputStream(16 * 2048)
                if (which == Util.XZ) {
                    `in` = XZInputStream(input)
                } else if (which == Util.LZMA) {
                    `in` = LZMAInputStream(input)
                }
                var size: Int
                try {
                    size = `in`!!.read()
                    while (size != -1) {
                        output.write(size)
                        size = `in`!!.read()
                    }
                } catch (e: CorruptedInputException) {
                    // Taking property byte
                    val properties = Arrays.copyOfRange(inputBytes, 0, 1)
                    // Taking dict size bytes
                    val dictSize = Arrays.copyOfRange(inputBytes, 1, 5)
                    // Taking uncompressed size bytes
                    val uncompressedSize = Arrays.copyOfRange(inputBytes, 5, 13)

                    // Reversing bytes in header
                    val header =
                        Bytes.concat(properties, Util.revertBytes(dictSize), Util.revertBytes(uncompressedSize))
                    val payload = Arrays.copyOfRange(inputBytes, 13, inputBytes.size)

                    // Trying again
                    input = ByteArrayInputStream(Bytes.concat(header, payload))
                    output = ByteArrayOutputStream(2048)
                    if (which == Util.XZ) {
                        `in` = XZInputStream(input)
                    } else if (which == Util.LZMA) {
                        `in` = LZMAInputStream(input)
                    }
                    try {
                        size = `in`!!.read()
                        while (size != -1) {
                            output.write(size)
                            size = `in`!!.read()
                        }
                    } catch (ex: CorruptedInputException) {
                        println("CorruptedInputException. Msg: " + ex.message)
                    }

                } catch (e: EOFException) {

                }

                `in`!!.close()
                return output.toByteArray()
            } catch (ex: IOException) {
                Logger.getLogger(Util::class.java.name).log(Level.SEVERE, null, ex)
            }

            return null
        }
    }


}
