package com.hbm.dim;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

public class DebugTeleporter extends Teleporter {

	private final WorldServer worldServerInstance;

	private double x;
	private double y;
	private double z;

	private boolean grounded; // Should we be placed directly on the first ground block below?

	public DebugTeleporter(WorldServer world, double x, double y, double z, boolean grounded) {
		super(world);
		this.worldServerInstance = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.grounded = grounded;
	}

	@Override

	public void placeInPortal(Entity pEntity, double p2, double p3, double p4, float p5) {
		int ix = (int)x;
		int iy = (int)y;
		int iz = (int)z;

		if(grounded) {
			for(int i = worldServerInstance.getHeight(); i > 0; i--) {
				if(worldServerInstance.getBlock(ix, i, iz) != Blocks.air) {
					y = i + 5;
					break;
				}
			}			
		} else {
			worldServerInstance.getBlock(ix, iy, iz); // dummy load to maybe gen chunk
		}

		pEntity.setPosition(x, y, z);
	}

	public static void teleport(EntityPlayer player, int dim, double x, double y, double z, boolean grounded) {
		if(player.dimension == dim) return; // ignore if we're teleporting to the same place

		MinecraftServer mServer = MinecraftServer.getServer();
		Side sidex = FMLCommonHandler.instance().getEffectiveSide();
		if (sidex == Side.SERVER) {
			if (player instanceof EntityPlayerMP) {
				EntityPlayerMP playerMP = (EntityPlayerMP) player;
				WorldServer sourceServer = playerMP.getServerForPlayer();
				WorldServer targetServer = (WorldServer) mServer.worldServerForDimension(dim);
				ServerConfigurationManager manager = playerMP.mcServer.getConfigurationManager();
				DebugTeleporter teleporter = new DebugTeleporter(targetServer, x, y, z, grounded);

				// Store these since they change after transfer
				int fromDimension = player.dimension;
				Entity ridingEntity = player.ridingEntity;

				manager.transferPlayerToDimension(playerMP, dim, teleporter);

				if(ridingEntity != null) {
					sourceServer.removeEntity(ridingEntity);
					ridingEntity.isDead = false;

					manager.transferEntityToWorld(ridingEntity, fromDimension, sourceServer, targetServer, teleporter);

					Entity newEntity = EntityList.createEntityByName(EntityList.getEntityString(ridingEntity), targetServer);
					if (newEntity != null) {
						newEntity.copyDataFrom(ridingEntity, true);
						targetServer.spawnEntityInWorld(newEntity);
					}

					ridingEntity.isDead = true;
					sourceServer.resetUpdateEntityTick();
					targetServer.resetUpdateEntityTick();

					player.mountEntity(newEntity);
				}
			}
		}
	}

}
