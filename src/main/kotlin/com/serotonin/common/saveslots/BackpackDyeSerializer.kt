@file:JvmName("BackpackDyeSerializerKt")

package com.serotonin.common.saveslots

import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.RecipeInputInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.SpecialRecipeSerializer


import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeType
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import net.minecraft.world.World

/*
@Suppress("UNCHECKED_CAST")
val FIXED_BACKPACK_DYE_SERIALIZER: RecipeSerializer<CraftingRecipe> =
    SpecialRecipeSerializer { category ->
        if (FabricLoader.getInstance().environmentType != EnvType.SERVER) {
            throw IllegalStateException("FIXED_BACKPACK_DYE_SERIALIZER called on client!")
        }
        try {
            val clazz = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.crafting.BackpackDyeRecipe")
            val constructor = clazz.getConstructor()
            constructor.newInstance() as CraftingRecipe

            /*
            val clazz = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.crafting.BackpackDyeRecipe")
            val obfCategoryClass = Class.forName("net.minecraft.class_7710")
            val constructor = clazz.getConstructor(obfCategoryClass)
            constructor.newInstance(obfCategoryClass.cast(category)) as CraftingRecipe


             */
        } catch (e: Exception) {
            throw RuntimeException("Failed to instantiate BackpackDyeRecipe reflectively", e)
        }
    }
*/



@JvmField
val FIXED_BACKPACK_DYE_SERIALIZER: RecipeSerializer<CraftingRecipe> =
    SpecialRecipeSerializer { category ->
        try {
            val clazz = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.crafting.BackpackDyeRecipe")
            val obfCategoryClass = Class.forName("net.minecraft.class_7710")
            val constructor = clazz.getConstructor(obfCategoryClass)


            constructor.newInstance(obfCategoryClass.cast(category)) as CraftingRecipe
        } catch (e: Exception) {
            throw RuntimeException("Failed to instantiate BackpackDyeRecipe", e)
            //DummyCraftingRecipe()
        }
    }
/*
class DummyCraftingRecipe : CraftingRecipe<RecipeInputInventory> {

    override fun matches(input: RecipeInputInventory, world: World): Boolean = false

    override fun craft(input: RecipeInputInventory, lookup: RegistryWrapper.WrapperLookup): ItemStack = ItemStack.EMPTY

    override fun fits(width: Int, height: Int): Boolean = false

    override fun getResult(lookup: RegistryWrapper.WrapperLookup): ItemStack = ItemStack.EMPTY

    override fun getId(): Identifier = Identifier.of("sophisticatedbackpacks", "backpack_dye_dummy")

    override fun getSerializer(): RecipeSerializer<*> = FIXED_BACKPACK_DYE_SERIALIZER

    override fun getType(): RecipeType<*> = RecipeType.CRAFTING

    override fun getCategory(): CraftingRecipeCategory = CraftingRecipeCategory.MISC
}*/