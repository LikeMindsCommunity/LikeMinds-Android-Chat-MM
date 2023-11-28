package com.likeminds.chatmm.member.view

import android.app.Activity
import android.content.Intent
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.likeminds.chatmm.R
import com.likeminds.chatmm.databinding.FragmentDmAllMembersBinding
import com.likeminds.chatmm.dm.model.CheckDMLimitViewData
import com.likeminds.chatmm.dm.model.DMLimitExceededDialogExtras
import com.likeminds.chatmm.dm.view.DMFeedFragment.Companion.DM_ALL_MEMBER_RESULT
import com.likeminds.chatmm.dm.view.DMLimitExceededDialogFragment
import com.likeminds.chatmm.member.model.CommunityMembersExtras
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.member.view.CommunityMembersActivity.Companion.DM_ALL_MEMBERS_EXTRAS
import com.likeminds.chatmm.member.view.adapter.CommunityMembersAdapter
import com.likeminds.chatmm.member.view.adapter.CommunityMembersAdapterListener
import com.likeminds.chatmm.member.viewmodel.CommunityMembersViewModel
import com.likeminds.chatmm.search.util.CustomSearchBar
import com.likeminds.chatmm.utils.*
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.BaseFragment
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class CommunityMembersFragment : BaseFragment<FragmentDmAllMembersBinding, CommunityMembersViewModel>(),
    CommunityMembersAdapterListener {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun getViewModelClass(): Class<CommunityMembersViewModel> {
        return CommunityMembersViewModel::class.java
    }

    override fun getViewBinding(): FragmentDmAllMembersBinding {
        return FragmentDmAllMembersBinding.inflate(layoutInflater)
    }

    companion object {
        const val TAG = "DMAllMemberFragment"
    }

    private lateinit var mAdapter: CommunityMembersAdapter

    private lateinit var scrollListener: EndlessRecyclerScrollListener
    private lateinit var extras: CommunityMembersExtras
    private var searchKeyword: String? = null

    override fun receiveExtras() {
        super.receiveExtras()
        val bundle = requireActivity().intent?.getBundleExtra("bundle")
        extras = bundle?.getParcelable(DM_ALL_MEMBERS_EXTRAS)
            ?: throw ErrorUtil.emptyExtrasException(TAG)
    }

    override fun setUpViews() {
        super.setUpViews()
        setupMenu()
        initRecyclerView()
        initData()
        initializeSearchView()
    }

    override fun observeData() {
        super.observeData()
        viewModel.membersResponse.observe(viewLifecycleOwner) { response ->
            val totalOnlyMembers = response.second
            val members = response.first

            setMembersCount(totalOnlyMembers)
            setMembers(members)
        }

        viewModel.checkDMLimitResponse.observe(viewLifecycleOwner) { response ->
            val checkDMLimitViewData = response.first
            val uuidSelected = response.second
            onDMLimitReceived(checkDMLimitViewData, uuidSelected)
        }

        viewModel.dmChatroomId.observe(viewLifecycleOwner) { chatroomId ->
            createActivityResultAndFinish(chatroomId)
        }

        viewModel.errorEventFlow.onEach { response ->
            when (response) {
                is CommunityMembersViewModel.ErrorMessageEvent.CheckDMLimit -> {
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }

                is CommunityMembersViewModel.ErrorMessageEvent.CreateDMChatroom -> {
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }

                is CommunityMembersViewModel.ErrorMessageEvent.GetAllMembers -> {
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }

                is CommunityMembersViewModel.ErrorMessageEvent.SearchMembers -> {
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }
            }

        }.observeInLifecycle(viewLifecycleOwner)
    }

    override fun onMemberSelected(member: MemberViewData) {
        val uuid = member.sdkClientInfo.uuid
        viewModel.checkDMLimit(uuid)
    }

    private fun initData() {
        viewModel.getAllMembers(extras.showList, 1)
    }

    private fun initRecyclerView() {
        //create layout manager
        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL

        //create scroll listener
        scrollListener = object : EndlessRecyclerScrollListener(linearLayoutManager) {
            override fun onLoadMore(currentPage: Int) {
                if (currentPage > 0) {
                    if (searchKeyword == null) {
                        viewModel.getAllMembers(extras.showList, currentPage)
                    } else {
                        viewModel.searchMembers(extras.showList, currentPage, searchKeyword ?: "")
                    }
                }
            }
        }

        //attach it to recycler view
        binding.rvMembers.apply {
            mAdapter = CommunityMembersAdapter(this@CommunityMembersFragment, userPreferences)
            adapter = mAdapter
            layoutManager = linearLayoutManager
            addOnScrollListener(scrollListener)
        }
    }

    private fun setupMenu() {
        val fragmentActivity = activity as AppCompatActivity?
        fragmentActivity?.setSupportActionBar(binding.toolbar)

        val menuHost: MenuHost = requireActivity() as MenuHost

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.dm_all_member_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_search -> {
                        showSearchToolbar()
                    }

                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setMembersCount(totalOnlyMembers: Int) {
        if (totalOnlyMembers == 0) return
        binding.tvMemberCount.apply {
            show()
            text = resources.getQuantityString(
                R.plurals.number_of_member,
                totalOnlyMembers,
                totalOnlyMembers,
                ""
            )
        }
    }

    private fun setMembers(members: List<MemberViewData>) {
        mAdapter.addAll(members)
    }

    private fun onDMLimitReceived(dmLimitData: CheckDMLimitViewData?, uuidSelected: String) {
        if (dmLimitData == null) return
        when {
            (dmLimitData.chatroomId != null) -> {
                createActivityResultAndFinish(dmLimitData.chatroomId)
            }

            (dmLimitData.isRequestDMLimitExceeded == false) -> {
                viewModel.createDMChatroom(uuidSelected)
            }

            else -> {
                val dmLimitExceededDialogExtras = DMLimitExceededDialogExtras.Builder()
                    .newRequestDMTimestamp(dmLimitData.newRequestDMTimestamp)
                    .duration(dmLimitData.duration)
                    .numberInDuration(dmLimitData.numberInDuration)
                    .build()
                DMLimitExceededDialogFragment.showDialog(
                    childFragmentManager,
                    dmLimitExceededDialogExtras
                )
            }
        }
    }

    private fun createActivityResultAndFinish(chatroomId: String) {
        val result = viewModel.createDMAllMemberResult(chatroomId)
        val intent = Intent().apply {
            putExtra(DM_ALL_MEMBER_RESULT, result)
        }
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    private fun showSearchToolbar() {
        binding.searchBar.apply {
            show()
            post {
                openSearch()
            }
        }
    }

    private fun initializeSearchView() {
        binding.searchBar.apply {
            this.initialize(lifecycleScope)

            setSearchViewListener(object : CustomSearchBar.SearchViewListener {
                override fun onSearchViewClosed() {
                    binding.searchBar.hide()
                    clearMemberSearch()
                }

                override fun crossClicked() {
                    clearMemberSearch()
                }

                override fun keywordEntered(keyword: String) {
                    scrollListener.resetData()
                    mAdapter.clearAndNotify()
                    searchKeyword = keyword
                    viewModel.searchMembers(extras.showList, 1, searchKeyword ?: "")
                }

                override fun emptyKeywordEntered() {
                    clearMemberSearch()
                }
            })
            observeSearchView(true)
        }
    }

    private fun clearMemberSearch() {
        scrollListener.resetData()
        mAdapter.clearAndNotify()
        searchKeyword = null
        viewModel.getAllMembers(extras.showList, 1)
    }
}