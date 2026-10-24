package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3d;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Environment(EnvType.CLIENT)
public class ParticleEffectUtil {
    private static final Cache<String, ParticleOptions> particleCache = CacheBuilder.newBuilder().expireAfterAccess(60, TimeUnit.SECONDS).build();

    public static boolean handleParticle(ExecutionContext<IContext<Entity>> context, Function.ArgumentCollection arguments, boolean isAbsolute) throws ExecutionException, CommandSyntaxException {
        String particleId = arguments.getAsString(context, 0);
        if (StringUtils.isBlank(particleId)) {
            return false;
        }

        Vector3d offset = new Vector3d(0.0d, 0.0d, 0.0d);
        Vector3d delta = new Vector3d(0.0d, 0.0d, 0.0d);
        double speed = 0.0d;
        int count = 0;
        int lifetime = 20;
        int argCount = arguments.size();

        if (argCount > 1) {
            offset.x = arguments.getAsDouble(context, 1);
        }
        if (argCount > 2) {
            offset.y = arguments.getAsDouble(context, 2);
        }
        if (argCount > 3) {
            offset.z = arguments.getAsDouble(context, 3);
        }
        if (argCount > 4) {
            delta.x = arguments.getAsDouble(context, 4);
        }
        if (argCount > 5) {
            delta.y = arguments.getAsDouble(context, 5);
        }
        if (argCount > 6) {
            delta.z = arguments.getAsDouble(context, 6);
        }
        if (argCount > 7) {
            speed = arguments.getAsDouble(context, 7);
        }
        if (argCount > 8) {
            count = Math.max(arguments.getAsInt(context, 8), 0);
        }
        if (argCount > 9) {
            lifetime = Math.max(arguments.getAsInt(context, 9), 1);
        }

        spawnParticles(context.entity().entity(), particleId, offset, delta, speed, count, lifetime, isAbsolute, context.entity().random());
        return true;
    }

    private static void spawnParticles(Entity entity, String particleId, Vector3d offset, Vector3d delta, double speed, int count, int lifetime, boolean isAbsolute, RandomSource random) throws ExecutionException, CommandSyntaxException {
        ParticleOptions particleOptions = particleCache.get(particleId, () -> {
            return readParticle(new StringReader(particleId), entity.level().registryAccess());
        });

        if (particleOptions == null) {
            return;
        }

        ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;

        if (count == 0) {
            Vec3 spawnPos = new Vec3(offset.x(), offset.y(), offset.z());
            if (!isAbsolute) {
                if (entity instanceof Player) {
                    spawnPos = spawnPos.yRot((-((Player) entity).yBodyRot) * 0.017453292f);
                } else {
                    spawnPos = spawnPos.yRot((-entity.getYRot()) * 0.017453292f);
                }
            }

            double x = entity.getX() + spawnPos.x();
            double y = entity.getY() + spawnPos.y();
            double z = entity.getZ() + spawnPos.z();
            double velocityX = speed * delta.x();
            double velocityY = speed * delta.y();
            double velocityZ = speed * delta.z();

            Minecraft.getInstance().execute(() -> {
                Particle particle = particleEngine.createParticle(withParticleColor(particleOptions, velocityX, velocityY, velocityZ), x, y, z, velocityX, velocityY, velocityZ);
                if (particle != null) {
                    particle.setLifetime(lifetime);
                }
            });
            return;
        }

        for (int i = 0; i < count; i++) {
            emitParticle(entity, offset, delta, speed, lifetime, particleEngine, particleOptions, isAbsolute, random);
        }
    }

    private static ParticleOptions readParticle(StringReader reader, HolderLookup.Provider registries) throws CommandSyntaxException {
        int start = reader.getCursor();
        Identifier id = Identifier.read(reader);
        if (id.equals(Identifier.withDefaultNamespace("ambient_entity_effect"))) return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0x26FFFFFF);
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getOptional(id).orElseThrow(() -> ParticleArgument.ERROR_UNKNOWN_PARTICLE.createWithContext(reader, id));
        if (type instanceof SimpleParticleType simple) return simple;
        if (type == ParticleTypes.ENTITY_EFFECT) return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFFFFFFFF);
        if (type == ParticleTypes.FLASH) return ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF);
        if (type == ParticleTypes.DUST) return new DustParticleOptions(readColor(reader), readFloat(reader));
        if (type == ParticleTypes.DUST_COLOR_TRANSITION) {
            int from = readColor(reader);
            float scale = readFloat(reader);
            return new DustColorTransitionOptions(from, readColor(reader), scale);
        }
        if (type == ParticleTypes.BLOCK || type == ParticleTypes.BLOCK_MARKER || type == ParticleTypes.FALLING_DUST) {
            reader.expect(' ');
            var state = BlockStateParser.parseForBlock(registries.lookupOrThrow(Registries.BLOCK), reader, false).blockState();
            if (type == ParticleTypes.BLOCK) return new BlockParticleOption(ParticleTypes.BLOCK, state);
            if (type == ParticleTypes.BLOCK_MARKER) return new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state);
            return new BlockParticleOption(ParticleTypes.FALLING_DUST, state);
        }
        if (type == ParticleTypes.ITEM) {
            reader.expect(' ');
            var input = new ItemParser(registries).parse(reader);
            if (input.item().is(Items.AIR.builtInRegistryHolder())) throw ParticleArgument.ERROR_INVALID_OPTIONS.createWithContext(reader, "Item must be non-empty");
            if (!reader.canRead() || reader.peek() != '{') return new ItemParticleOption(ParticleTypes.ITEM, new ItemStackTemplate(input.item(), 1, input.components()));
            CompoundTag stack = new CompoundTag();
            stack.putString("id", BuiltInRegistries.ITEM.getKey(input.item().value()).toString());
            stack.putByte("Count", (byte) 1);
            stack.put("tag", TagParser.parseCompoundAsArgument(reader));
            var updated = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, stack), 3465, SharedConstants.getCurrentVersion().dataVersion().version());
            var item = ItemStackTemplate.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), updated.getValue()).getOrThrow(ParticleArgument.ERROR_INVALID_OPTIONS::create);
            return new ItemParticleOption(ParticleTypes.ITEM, item);
        }
        if (type == ParticleTypes.VIBRATION) {
            reader.expect(' ');
            float x = (float) reader.readDouble();
            reader.expect(' ');
            float y = (float) reader.readDouble();
            reader.expect(' ');
            float z = (float) reader.readDouble();
            reader.expect(' ');
            return new VibrationParticleOption(new BlockPositionSource(BlockPos.containing(x, y, z)), reader.readInt());
        }
        if (type == ParticleTypes.SCULK_CHARGE) return new SculkChargeParticleOptions(readFloat(reader));
        if (type == ParticleTypes.SHRIEK) {
            reader.expect(' ');
            return new ShriekParticleOption(reader.readInt());
        }
        reader.setCursor(start);
        return ParticleArgument.readParticle(reader, registries);
    }

    private static ParticleOptions withParticleColor(ParticleOptions options, double red, double green, double blue) {
        if (options instanceof ColorParticleOption color && color.getType() == ParticleTypes.ENTITY_EFFECT) {
            return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.colorFromFloat(color.getAlpha(), Mth.clamp((float) red, 0.0f, 1.0f), Mth.clamp((float) green, 0.0f, 1.0f), Mth.clamp((float) blue, 0.0f, 1.0f)));
        }
        return options;
    }

    private static float readFloat(StringReader reader) throws CommandSyntaxException {
        reader.expect(' ');
        return reader.readFloat();
    }

    private static int readColor(StringReader reader) throws CommandSyntaxException {
        return ARGB.colorFromFloat(1.0f, Mth.clamp(readFloat(reader), 0.0f, 1.0f), Mth.clamp(readFloat(reader), 0.0f, 1.0f), Mth.clamp(readFloat(reader), 0.0f, 1.0f));
    }

    private static void emitParticle(Entity entity, Vector3d offset, Vector3d delta, double speed, int lifetime, ParticleEngine particleEngine, ParticleOptions particleOptions, boolean isAbsolute, RandomSource random) {
        double spreadX = random.nextGaussian() * delta.x();
        double spreadY = random.nextGaussian() * delta.y();
        double spreadZ = random.nextGaussian() * delta.z();
        double velocityX = random.nextGaussian() * speed;
        double velocityY = random.nextGaussian() * speed;
        double velocityZ = random.nextGaussian() * speed;

        Vec3 spawnPos = new Vec3(offset.x() + spreadX, offset.y() + spreadY, offset.z() + spreadZ);

        if (!isAbsolute) {
            spawnPos = spawnPos.yRot((-entity.getYRot()) * 0.017453292f);
        }

        double x = entity.getX() + spawnPos.x();
        double y = entity.getY() + spawnPos.y();
        double z = entity.getZ() + spawnPos.z();

        Minecraft.getInstance().execute(() -> {
            Particle particle = particleEngine.createParticle(withParticleColor(particleOptions, velocityX, velocityY, velocityZ), x, y, z, velocityX, velocityY, velocityZ);
            if (particle != null) {
                particle.setLifetime(lifetime);
            }
        });
    }
}