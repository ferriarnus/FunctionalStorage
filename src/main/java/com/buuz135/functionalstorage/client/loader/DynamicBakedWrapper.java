package com.buuz135.functionalstorage.client.loader;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Class from Mantle {@url https://github.com/SlimeKnights/Mantle/blob/1.18.2/src/main/java/slimeknights/mantle/client/}
 * <p>
 * Used to create a baked model wrapper that has a dynamic {@link #getQuads(BlockState, Direction, RandomSource, ModelData, RenderType)} without worrying about overriding the deprecated variant.
 *
 * @param <T> Baked model parent
 */
@SuppressWarnings("WeakerAccess")
public abstract class DynamicBakedWrapper<T extends BakedModel> extends BakedModelWrapper<T> {

    protected DynamicBakedWrapper(T originalModel) {
        super(originalModel);
    }


    @Override
    public abstract List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, RenderType renderType);


}
