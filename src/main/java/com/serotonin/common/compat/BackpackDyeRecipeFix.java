package com.serotonin.common.compat;

import com.serotonin.common.saveslots.BackpackDyeSerializerKt;
import net.minecraft.recipe.RecipeSerializer;
/*
public interface BackpackDyeRecipeFix {
    @SuppressWarnings("unused")
    default RecipeSerializer<?> getSerializer() {
        System.out.println("Mixin override: getSerializer() for BackpackDyeRecipe");
        return BackpackDyeSerializerKt.getFIXED_BACKPACK_DYE_SERIALIZER();
    }
}
*/

/*
public interface BackpackDyeRecipeFix {
    default RecipeSerializer<?> getSerializer() {
        return BackpackDyeSerializerKt.getFIXED_BACKPACK_DYE_SERIALIZER();
    }
}*/

public interface BackpackDyeRecipeFix {
    @SuppressWarnings("unused")
    default RecipeSerializer<?> getSerializer() {
        System.out.println("Overridden getSerializer via interface bridge");
        return BackpackDyeSerializerKt.FIXED_BACKPACK_DYE_SERIALIZER;
    }
}