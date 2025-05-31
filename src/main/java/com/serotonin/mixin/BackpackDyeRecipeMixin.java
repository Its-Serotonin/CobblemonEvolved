package com.serotonin.mixin;

import com.serotonin.common.compat.BackpackDyeRecipeFix;
import com.serotonin.common.saveslots.BackpackDyeSerializerKt;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.input.RecipeInput;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.serotonin.common.saveslots.BackpackDyeSerializerKt.*;


@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.crafting.BackpackDyeRecipe")
public abstract class BackpackDyeRecipeMixin implements BackpackDyeRecipeFix {
}