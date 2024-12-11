package com.noxis.tabsyncsample.mediator

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING
import com.google.android.material.tabs.TabLayout

typealias Page = Pair<Int, Int>

/**
 * TabLayoutMediator для работы с одним TabLayout
 * recyclerView и tabLayout — то, что мы обвесим листенерами и синхронизируем
 * tabFactory нужен для верстки табов
 * indicesProvider — возвращает список индексов ячеек
 */
class TabLayoutMediator(
    private val recyclerView: RecyclerView,
    private val tabLayout: TabLayout,
    private val tabFactory: (tab: TabLayout.Tab, position: Int) -> Unit,
    private val indicesProvider: () -> List<Int>,
    private val tabSelectListener: (categoryTabIndex: Int, categoryListIndex: Int) -> Unit = { categoryTabIndex: Int, categoryListIndex -> },
) {
    private var pagerAdapterObserver: PagerAdapterObserver? = null
    private val smoothScroller: RecyclerView.SmoothScroller by lazy {
        object : LinearSmoothScroller(recyclerView.context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }
    }

    private var cellIndices: List<Int> = emptyList()
    private var pages: List<Page> = emptyList()

    // Поля для контроля состояния скролла
    private var previousScrollState = SCROLL_STATE_IDLE
    private var scrollState = SCROLL_STATE_IDLE
    private var isScrollByTabClick: Boolean = false
    private var chosenPage: Page? = Pair(0, 0)

    //Для отслеживания скролла
    private val onScrollListener = RecyclerViewScrollListener(
        //Надо понимать, из какого и в какое состояние переходит скролл
        //нужно смотреть не только текущее состояние скролла, но и сравнивать его с предыдущим
        onStateChanged = { _, newState ->
            previousScrollState = scrollState
            scrollState = newState

            val isDraggingNow = newState == SCROLL_STATE_DRAGGING
            val isSettlingAfterClick =
                newState == SCROLL_STATE_SETTLING && previousScrollState == SCROLL_STATE_IDLE
            val isSettlingAfterScroll =
                newState == SCROLL_STATE_SETTLING && previousScrollState == SCROLL_STATE_DRAGGING
            val isScrollFinished = newState == SCROLL_STATE_IDLE

            when {
                isDraggingNow || isSettlingAfterScroll -> {
                    isScrollByTabClick = false
                }

                isSettlingAfterClick -> {
                    isScrollByTabClick = true
                }

                isScrollFinished -> {
                    isScrollByTabClick = false
                }
            }
        },
        //производится поиск индекса ячейки и выбор таба
        onScroll = { recyclerView, _, _ ->

            if (isScrollByTabClick) return@RecyclerViewScrollListener

            if (isScrolling()) {
                val linearLayoutManager: LinearLayoutManager =
                    recyclerView.layoutManager as LinearLayoutManager

                val lastVisibleCellIndex =
                    linearLayoutManager.findLastVisibleItemPosition()

                val itemCount = linearLayoutManager.itemCount

                //список проскроллен до конца, но последний таб не выбирается.
                // Для этого делаем отдельную проверку
                if (lastVisibleCellIndex == itemCount - 1) {
                    selectTabBy(cellIndices.lastIndex)
                } else {
                    //ищем первый видимый элемент в списке
                    var firstVisibleCellIndex =
                        linearLayoutManager.findFirstCompletelyVisibleItemPosition()

                    if (firstVisibleCellIndex == -1) {
                        firstVisibleCellIndex =
                            linearLayoutManager.findFirstVisibleItemPosition()
                    }
                    //смотрим к какому табу он относится
                    val page =
                        pages.firstOrNull { (startPageIndex, endPageIndex) ->
                            //Индекс таба находится по принадлежности к странице
                            //помогает отслеживать скролл в обе стороны
                            firstVisibleCellIndex in startPageIndex..endPageIndex
                        }

                    if (chosenPage == page) {
                        return@RecyclerViewScrollListener
                    }

                    chosenPage = page
                    page?.let { (startPageIndex, endPageIndex) ->
                        val categoryTabIndex = cellIndices.indexOf(startPageIndex)
                        selectTabBy(categoryTabIndex)
                        //Когда находим, выбираем этот таб
                        tabSelectListener(categoryTabIndex, startPageIndex)
                    }
                }
            }
        }
    )

    /**
     * производит инициализацию всех листенеров и заполняет табы.
     */
    fun attach() {
        pagerAdapterObserver =
            PagerAdapterObserver { populateTabs() } //Для автоматического перезаполнения TabLayout назначим recyclerView PagerAdapterObserver
        pagerAdapterObserver?.let { recyclerView.adapter?.registerAdapterDataObserver(it) }
        recyclerView.addOnScrollListener(onScrollListener)
        populateTabs()

        //Вызываем для получения индексов ячеек
        cellIndices = indicesProvider()

        //разобьём список индексов на страницы(нужно для отслеживания скролла в обе стороны)
        pages = cellIndices.zipWithNext { current, next ->
            current to next - 1
        } + (cellIndices.last() to Int.MAX_VALUE)

        //установим листенер на TabLayout
        setTabsClickListener { tabIndex ->
            if (isScrolling()) {
                return@setTabsClickListener
            }
            val cellIndex = cellIndices[tabIndex]
            tabSelectListener(tabIndex, cellIndex)
            smoothScroller.targetPosition = cellIndex
            recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
        }
    }

    /**
     * всё очищает
     */
    fun detach() {
        pagerAdapterObserver?.let { recyclerView.adapter?.unregisterAdapterDataObserver(it) }
        pagerAdapterObserver = null
        recyclerView.removeOnScrollListener(onScrollListener)
        tabLayout.clearOnTabSelectedListeners()
        tabLayout.removeAllTabs()
    }

    private fun setTabsClickListener(onClick: (tabIndex: Int) -> Unit) {
        tabLayout.addOnTabSelectedListener(TabSelectListener(
            onSelected = {
                onClick(it.position)
            }
        ))
    }

    private fun selectTabBy(index: Int) {
        if (tabLayout.getTabAt(index)?.isSelected == false) {
            tabLayout.getTabAt(index)?.select()
        }
    }

    private fun isScrolling(): Boolean {
        return scrollState == SCROLL_STATE_DRAGGING
                || scrollState == SCROLL_STATE_SETTLING
    }

    private fun populateTabs() {
        tabLayout.removeAllTabs()

        val adapter = recyclerView.adapter
        val indicesCount: Int = indicesProvider().size
        if (adapter != null) {
            for (i in 0 until indicesCount) {
                val tab = tabLayout.newTab()
                tabFactory(tab, i)
                tabLayout.addTab(tab, false)
            }
        }
    }
}