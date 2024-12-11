package com.noxis.tabsyncsample.list

import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import com.noxis.tabsyncsample.R
import com.noxis.tabsyncsample.databinding.ItemCategoryBinding
import com.noxis.tabsyncsample.databinding.ItemItemBinding
import com.noxis.tabsyncsample.model.Category
import com.noxis.tabsyncsample.model.Item
import ru.surfstudio.android.easyadapter.controller.BindableItemController
import ru.surfstudio.android.easyadapter.holder.BindableViewHolder

class CategoryController(
    private val viewType: Int
) : BindableItemController<Category, CategoryController.Holder>() {


    class Holder(parent: ViewGroup) : BindableViewHolder<Category>(parent, R.layout.item_category) {

        private val binding: ItemCategoryBinding by viewBinding(ItemCategoryBinding::bind)

        override fun bind(category: Category) {
            binding.categoryName.text = category.name
        }
    }

    override fun createViewHolder(parent: ViewGroup): Holder {
        return Holder(parent)
    }

    override fun getItemId(data: Category): Any {
        return data.name
    }

    override fun viewType(): Int {
        return viewType
    }

    override fun getItemHash(data: Category?): Any {
        return data.hashCode()
    }
}


class ItemController: BindableItemController<Item, ItemController.Holder>() {


    class Holder(parent: ViewGroup) : BindableViewHolder<Item>(parent, R.layout.item_item) {

        private val binding: ItemItemBinding by viewBinding(ItemItemBinding::bind)

        override fun bind(category: Item) {
            binding.itemNameTv.text = category.itemName
        }
    }

    override fun createViewHolder(parent: ViewGroup): Holder {
        return Holder(parent)
    }

    override fun getItemId(data: Item): Any {
        return data.itemName
    }

    override fun getItemHash(data: Item?): Any {
        return data.hashCode()
    }

}