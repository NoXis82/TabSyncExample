package com.noxis.tabsyncsample

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.color.MaterialColors
import com.noxis.tabsyncsample.databinding.ActivityMainBinding
import com.noxis.tabsyncsample.databinding.TabCategoryItemBinding
import com.noxis.tabsyncsample.databinding.TabSubCategoryItemBinding
import com.noxis.tabsyncsample.list.CategoryController
import com.noxis.tabsyncsample.list.ItemController
import com.noxis.tabsyncsample.mediator.TabLayoutMediator
import com.noxis.tabsyncsample.mock.categories
import com.noxis.tabsyncsample.mock.generateItems
import com.noxis.tabsyncsample.utils.replaceRipple
import com.noxis.tabsyncsample.utils.setTabIndicatorColor
import com.noxis.tabsyncsample.utils.toPx
import ru.surfstudio.android.easyadapter.EasyAdapter
import ru.surfstudio.android.easyadapter.ItemList
import com.google.android.material.R as materialR

class MainActivity : AppCompatActivity() {

    private val viewBinding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    private val tabCategoryIndicatorColor by lazy {
        MaterialColors.getColor(
            this,
            materialR.attr.colorTertiaryContainer,
            Color.BLACK
        )
    }

    private val tabCategoryIndicatorRippleColor by lazy {
        ColorUtils.setAlphaComponent(tabCategoryIndicatorColor, 0xB3) // 70% of alpha
    }

    private val categoryController = CategoryController(
        viewType = 100000
    )
    private val subCategoryController = CategoryController(
        viewType = 100001
    )

    private val itemController = ItemController()
    private val easyAdapter = EasyAdapter()

    private val indicesMap: MutableMap<Int, List<Int>> = mutableMapOf()
    private var itemsList: ItemList? = null
    private var childTabLayoutMediator: TabLayoutMediator? = null

    //Подключим родительский TabLayoutMediator
    private val parentTabLayoutMediator: TabLayoutMediator by lazy {
        TabLayoutMediator(
            recyclerView = viewBinding.recyclerView,
            tabLayout = viewBinding.categoriesParentLayout,
            tabFactory = { tab, position ->
                tab.apply {
                    customView =
                        TabCategoryItemBinding.inflate(LayoutInflater.from(this@MainActivity))
                            .apply { this.tabName.text = categories[position].name }
                            .root

                    view.replaceRipple(
                        backgroundCornerRadius = 12f.toPx,
                        backgroundColor = tabCategoryIndicatorColor,
                        rippleColor = tabCategoryIndicatorRippleColor
                    )
                }
            },
            indicesProvider = {
                indicesMap.keys.toList()
            },
            tabSelectListener = { tabIndex, categoryIndex ->
                createChildTabLayout(tabIndex, categoryIndex)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        initRecyclerView()
        inflateItems()
        inflateIndexesMap()
        viewBinding.categoriesParentLayout.setTabIndicatorColor(tabCategoryIndicatorColor)
        viewBinding.categoriesChildLayout.setTabIndicatorColor(tabCategoryIndicatorColor)
        parentTabLayoutMediator.attach()
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
    }

    private fun inflateItems() {
        itemsList = ItemList.create()
            .apply {
                categories.forEach { category ->
                    add(category, categoryController)
                    if (category.subCategories.isEmpty()) {
                        addAll(generateItems(), itemController)
                    }
                    category.subCategories.forEach { subCategory ->
                        add(subCategory, subCategoryController)
                        addAll(generateItems(), itemController)
                    }
                }
            }
            .also(easyAdapter::setItems)
    }

    private fun initRecyclerView() {
        with(viewBinding.recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = easyAdapter
        }
    }

    //При выборе родительского таба дочерний TabLayoutMediator будет перезаполняться подкатегориями
    private fun createChildTabLayout(categoryTabIndex: Int, categoryListIndex: Int) {
        childTabLayoutMediator?.detach()
        childTabLayoutMediator = null
        val childIndices = indicesMap[categoryListIndex]
        if (childIndices.isNullOrEmpty()) {
            return
        }
        childTabLayoutMediator = TabLayoutMediator(
            recyclerView = viewBinding.recyclerView,
            tabLayout = viewBinding.categoriesChildLayout,
            tabFactory = { tab, position ->
                tab.apply {
                    customView =
                        TabSubCategoryItemBinding.inflate(LayoutInflater.from(this@MainActivity))
                            .apply {
                                this.tabName.text =
                                    categories[categoryTabIndex].subCategories[position].name
                            }
                            .root

                    view.replaceRipple(
                        backgroundCornerRadius = 36f.toPx,
                        backgroundColor = tabCategoryIndicatorColor,
                        rippleColor = tabCategoryIndicatorRippleColor
                    )
                }
            },
            indicesProvider = { childIndices }
        ).apply { attach() }
    }

    /**
     * Соберём индексы категорий и подкатегорий. Важно делать это каждый раз после заполнения
     * и изменения списка, чтобы при скролле списка табы выбирались корректно.
     */
    private fun inflateIndicesMap() {
        val parentIndices = itemsList?.getIndicesByViewType(categoryController.viewType()) ?: emptyList()
        val childIndices = itemsList?.getIndicesByViewType(subCategoryController.viewType()) ?: emptyList()

        val indexPairs = parentIndices.zipWithNext { current, next ->
            current to childIndices.filter { it in current until next }
        } + (parentIndices.last() to childIndices.filter { it in parentIndices.last() until Int.MAX_VALUE })

        indicesMap.clear()
        indicesMap.putAll(indexPairs)
    }

    private fun ItemList?.getIndicesByViewType(viewType: Int): List<Int> {
        return this?.withIndex()
            ?.filter { (_, value) -> value.itemController.viewType() == viewType }
            ?.map { it.index } ?: emptyList()
    }

    //Заполним список данными
    private fun inflateIndexesMap() {
        val parentIndices = itemsList?.getIndicesByViewType(categoryController.viewType()) ?: emptyList()
        val childIndices = itemsList?.getIndicesByViewType(subCategoryController.viewType()) ?: emptyList()

        val indexPairs = parentIndices.zipWithNext { current, next ->
            current to childIndices.filter { it in current until next }
        } + (parentIndices.last() to childIndices.filter { it in parentIndices.last() until Int.MAX_VALUE })

        indicesMap.clear()
        indicesMap.putAll(indexPairs)
    }
}