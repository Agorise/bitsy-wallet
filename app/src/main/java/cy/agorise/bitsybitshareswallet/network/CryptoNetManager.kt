package cy.agorise.bitsybitshareswallet.network

import cy.agorise.bitsybitshareswallet.enums.CryptoNet
import java.util.*



abstract class CryptoNetManager {

    /*
    * Utility for above methods
    *
    * */
    fun addCryptoNetURL(
        crypto: CryptoNet,
        urls: Array<String>
    ) {

        if (!CryptoNetURLs.containsKey(crypto)) {
            CryptoNetURLs[crypto] = HashSet()
        }

        val verifier = CryptoNetVerifier.getNetworkVerify(crypto)
        for (url in urls) {
            CryptoNetURLs[crypto]!!.add(url)
            verifier?.checkURL(url)
        }

    }

    fun removeCryptoNetURL(crypto: CryptoNet, url: String) {
        if (CryptoNetURLs.containsKey(crypto)) {
            CryptoNetURLs[crypto]!!.remove(url)
        }
    }

    private class TestedURL(val time: Long, internal val url: String) : Comparable<TestedURL> {

        override fun compareTo(o: TestedURL): Int {

            if (this === o) return 0
            return if (o !is TestedURL) 0 else (this.time - o.time).toInt()

        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o !is TestedURL) return false

            val testedURL = o as TestedURL?

            return url == testedURL!!.url
        }

        override fun hashCode(): Int {
            return url.hashCode()
        }
    }


    companion object {

        fun addCryptoNetURL(crypto: CryptoNet, url: String) {
            if (!CryptoNetURLs.containsKey(crypto)) {
                CryptoNetURLs[crypto] = HashSet()
            }

            CryptoNetURLs.put(crypto,HashSet<String>())
            val verifier = CryptoNetVerifier.getNetworkVerify(crypto)
            verifier?.checkURL(url)
        }

        /*
    * Utility for above methods
    *
    * */
        fun addCryptoNetURL(
            crypto: CryptoNet,
            urls: Array<String>
        ) {

            if (!CryptoNetURLs.containsKey(crypto)) {
                CryptoNetURLs[crypto] = HashSet()
            }

            val verifier = CryptoNetVerifier.getNetworkVerify(crypto)
            for (url in urls) {
                CryptoNetURLs.get(crypto)!!.add(url);
                verifier?.checkURL(url)
            }

        }
        fun getChaindId(crypto: CryptoNet): String? {
            val verifier = CryptoNetVerifier.getNetworkVerify(crypto)
            return verifier?.chainId
        }

        fun getURL(crypto: CryptoNet): String? {
            return getURL(crypto, 0)
        }

        fun getURL(crypto: CryptoNet, index: Int): String? {
            if (TestedURLs.containsKey(crypto) && TestedURLs[crypto]!!.size > index) {
                println("Servers url list " + Arrays.toString(TestedURLs[crypto]!!.toTypedArray()))
                return TestedURLs[crypto]!!.get(index).url
            }
            System.out.println("Servers " + crypto.label + " dioesn't have testedurl")

            return if (CryptoNetURLs.containsKey(crypto) && !CryptoNetURLs[crypto]!!.isEmpty()) {
                CryptoNetURLs[crypto]!!.iterator().next()
            } else null
        }

        /**
         * This map contains the list of the urls to be tested
         */
        private val CryptoNetURLs = HashMap<CryptoNet, HashSet<String>>()

        /**
         * This map contains the list of urls been tested and ordered by the fastests
         */
        private val TestedURLs = HashMap<CryptoNet, ArrayList<TestedURL>>()

        fun verifiedCryptoNetURL(crypto: CryptoNet, url: String, time: Long) {
            if (CryptoNetURLs.containsKey(crypto) && CryptoNetURLs[crypto]!!.contains(url)) {
                if (!TestedURLs.containsKey(crypto)) {
                    TestedURLs[crypto] = ArrayList<TestedURL>()
                }
                val testedUrl = TestedURL(time, url)
                if (!TestedURLs[crypto]!!.contains(testedUrl)) {
                    TestedURLs[crypto]!!.add(testedUrl)
                    Collections.sort<TestedURL>(TestedURLs[crypto])
                }
            } else {
                //TODO add error handler
            }
        }
    }

}