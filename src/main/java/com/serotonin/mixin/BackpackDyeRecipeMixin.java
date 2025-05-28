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

/*
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.crafting.BackpackDyeRecipe")
public abstract class BackpackDyeRecipeMixin {

    @Inject(method = "method_8119", at = @At("HEAD"), cancellable = true, remap = false)
    private void fixSerializer(CallbackInfoReturnable<RecipeSerializer<?>> cir) {
        cir.setReturnValue(BackpackDyeSerializerKt.FIXED_BACKPACK_DYE_SERIALIZER);
    }
}*/
/*
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.crafting.BackpackDyeRecipe")
public abstract class BackpackDyeRecipeMixin implements com.serotonin.common.compat.BackpackDyeRecipeFix {
}*/

/*
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.crafting.BackpackDyeRecipe")
@Implements(@Interface(iface = Recipe.class, prefix = "recipe$"))
public abstract class BackpackDyeRecipeMixin {

    @Unique
    public RecipeSerializer<?> recipe$getSerializer() {
        return getFIXED_BACKPACK_DYE_SERIALIZER();
    }
}*/

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.crafting.BackpackDyeRecipe")
public abstract class BackpackDyeRecipeMixin implements BackpackDyeRecipeFix {
}