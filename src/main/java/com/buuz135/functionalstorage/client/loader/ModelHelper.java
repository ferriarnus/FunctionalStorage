package com.buuz135.functionalstorage.client.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Class from Mantle {@url https://github.com/SlimeKnights/Mantle/blob/1.18.2/src/main/java/slimeknights/mantle/client/}
 *
 * Utilities to help in custom models
 */

public class ModelHelper {
    private static final Map<Block,ResourceLocation> TEXTURE_NAME_CACHE = new ConcurrentHashMap<>();
    /** Listener instance to clear cache */
    public static final ResourceManagerReloadListener LISTENER = manager -> TEXTURE_NAME_CACHE.clear();

    /* Baked models */

    /**
     * Gets the model for the given block
     * @param state  Block state
     * @param clazz  Class type to cast result into
     * @param <T>    Class type
     * @return  Block model, or null if its missing or the wrong class type
     */
    @Nullable
    public static <T extends BakedModel> T getBakedModel(BlockState state, Class<T> clazz) {
        Minecraft minecraft = Minecraft.getInstance();
        //noinspection ConstantConditions  null during run data
        if (minecraft == null) {
            return null;
        }
        BakedModel baked = minecraft.getModelManager().getBlockModelShaper().getBlockModel(state);
        // map multipart and weighted random into the first variant
        if (baked instanceof MultiPartBakedModel) {
            baked = ((MultiPartBakedModel)baked).selectors.get(0).getRight();
        }
        if (baked instanceof WeightedBakedModel) {
            baked = ((WeightedBakedModel) baked).wrapped;
        }
        // final model should match the desired type
        if (clazz.isInstance(baked)) {
            return clazz.cast(baked);
        }
        return null;
    }

    /**
     * Gets the model for the given item
     * @param item   Item provider
     * @param clazz  Class type to cast result into
     * @param <T>    Class type
     * @return  Item model, or null if its missing or the wrong class type
     */
    @Nullable
    public static <T extends BakedModel> T getBakedModel(ItemLike item, Class<T> clazz) {
        Minecraft minecraft = Minecraft.getInstance();
        //noinspection ConstantConditions  null during run data
        if (minecraft == null) {
            return null;
        }
        BakedModel baked = minecraft.getItemRenderer().getItemModelShaper().getItemModel(item.asItem());
        if (clazz.isInstance(baked)) {
            return clazz.cast(baked);
        }
        return null;
    }

    /**
     * Gets the texture name for a block from the model manager
     * @param block  Block to fetch
     * @return Texture name for the block
     */
    @SuppressWarnings("deprecation")
    private static ResourceLocation getParticleTextureInternal(Block block) {
        return Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getParticleIcon().getName();
    }

    /**
     * Gets the name of a particle texture for a block, using the cached value if present
     * @param block Block to fetch
     * @return Texture name for the block
     */
    public static ResourceLocation getParticleTexture(Block block) {
        return TEXTURE_NAME_CACHE.computeIfAbsent(block, ModelHelper::getParticleTextureInternal);
    }

    public static ResourceLocation getParticleTexture(Item block) {
        return TEXTURE_NAME_CACHE.computeIfAbsent(Block.byItem(block), ModelHelper::getParticleTextureInternal);
    }

    /* JSON */

    /**
     * Converts a JSON float array to the specified object
     * @param json    JSON object
     * @param name    Name of the array in the object to fetch
     * @param size    Expected array size
     * @param mapper  Functon to map from the array to the output type
     * @param <T> Output type
     * @return  Vector3f of data
     * @throws JsonParseException  If there is no array or the length is wrong
     */
    public static <T> T arrayToObject(JsonObject json, String name, int size, Function<float[], T> mapper) {
        JsonArray array = GsonHelper.getAsJsonArray(json, name);
        if (array.size() != size) {
            throw new JsonParseException("Expected " + size + " " + name + " values, found: " + array.size());
        }
        float[] vec = new float[size];
        for(int i = 0; i < size; ++i) {
            vec[i] = GsonHelper.convertToFloat(array.get(i), name + "[" + i + "]");
        }
        return mapper.apply(vec);
    }

    /**
     * Converts a JSON array with 3 elements into a Vector3f
     * @param json  JSON object
     * @param name  Name of the array in the object to fetch
     * @return  Vector3f of data
     * @throws JsonParseException  If there is no array or the length is wrong
     */
    public static Vector3f arrayToVector(JsonObject json, String name) {
        return arrayToObject(json, name, 3, arr -> new Vector3f(arr[0], arr[1], arr[2]));
    }

    /**
     * Gets a rotation from JSON
     * @param json  JSON parent
     * @return  Integer of 0, 90, 180, or 270
     */
    public static int getRotation(JsonObject json, String key) {
        int i = GsonHelper.getAsInt(json, key, 0);
        if (i >= 0 && i % 90 == 0 && i / 90 <= 3) {
            return i;
        } else {
            throw new JsonParseException("Invalid '" + key + "' " + i + " found, only 0/90/180/270 allowed");
        }
    }
  /*
    public static BakedQuad colorQuad(int color, BakedQuad quad) {
        //ColorTransformer transformer = new ColorTransformer(color, quad);
        //quad.pipe(transformer);
        return transformer.build();
    }
    */
}
