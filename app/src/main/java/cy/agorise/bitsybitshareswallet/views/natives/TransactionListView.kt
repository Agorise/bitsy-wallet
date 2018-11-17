package cy.agorise.bitsybitshareswallet.views.natives

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.models.CryptoCoinTransactionExtended
import cy.agorise.bitsybitshareswallet.viewmodels.TransactionListViewModel

class TransactionListView : RelativeLayout {

    internal var mInflater: LayoutInflater

    /*
     * The root view of this view
     */
    internal lateinit var rootView: View
    /*
     * The list view that holds every transaction item
     */
    internal lateinit var listView: RecyclerView
    /*
     * The adapter for the previous list view
     */
    internal var listAdapter: TransactionListAdapter? = null

    internal var transactionListViewModel: TransactionListViewModel? = null

    /*
     * how much transactions will remain to show before the list loads more
     */
    private val visibleThreshold = 5
    /*
     * if true, the transaction list will be loading new data
     */
    private val loading = true

    /*
     * One of three constructors needed to be inflated from a layout
     */
    constructor(context: Context) : super(context) {
        this.mInflater = LayoutInflater.from(context)
        init()
    }

    /*
     * One of three constructors needed to be inflated from a layout
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.mInflater = LayoutInflater.from(context)
        init()
    }

    /*
     * One of three constructors needed to be inflated from a layout
     */
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        this.mInflater = LayoutInflater.from(context)
        init()
    }

    /*
     * Initializes this view
     */
    fun init() {
        rootView = mInflater.inflate(R.layout.transaction_list, this, true)
        this.listView = rootView.findViewById(R.id.transactionListView)

        val linearLayoutManager = LinearLayoutManager(this.context)
        this.listView.setLayoutManager(linearLayoutManager)
        //Prevents the list to start again when scrolling to the end
        // this.listView.setNestedScrollingEnabled(false);


        /*this.listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(!loading && linearLayoutManager.getItemCount() <= (linearLayoutManager.findLastVisibleItemPosition() + visibleThreshold)){
                    onLoadMore();
                    loading = true;
                }
            }
        });*/
    }

    //public void onLoadMore(){
    //    listAdapter.add();

    //}

    /*
     * Sets the elements data of this view
     *
     * @param data the transactions that will be showed to the user
     */
    fun setData(data: PagedList<CryptoCoinTransactionExtended>?, fragment: Fragment) {
        //Initializes the adapter of the transaction list
        if (this.listAdapter == null) {
            this.listAdapter = TransactionListAdapter(fragment)
            this.listView.setAdapter(this.listAdapter)
        }

        //Sets the data of the transaction list
        if (data != null) {
            this.listAdapter!!.submitList(data)
        }
    }


}