package cl.netgamer.villageinfo;

// mc184-189 = import net.minecraft.server.v1_8_R3.*;
import net.minecraft.server.v1_9_R1.AxisAlignedBB;
import net.minecraft.server.v1_9_R1.EntityIronGolem;
import net.minecraft.server.v1_9_R1.Village;
import net.minecraft.server.v1_9_R1.WorldServer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class VillageUtil
{
	// this class is just a static bridge methods container
	// made for with craftbukkit 1.8.3
	
	protected static Location getCenter(Village v, World w)
	{
		return new Location(w, v.a().getX(), v.a().getY(), v.a().getZ());
	}
	
	protected static int getRadius(Village v)
	{
		return v.b();
	}
	
	protected static int getNumVillagers(Village v)
	{
		return v.e();
	}
	
	protected static int getNumDoors(Village v)
	{
		return v.c();
	}
	
	// copied from private method j() (updateNumIronGolems())
	protected static int getNumGolems(Village v, WorldServer w)
	{
	    return w.a(EntityIronGolem.class, new AxisAlignedBB(v.a().getX() - v.b(), v.a().getY() - 4, v.a().getZ() - v.b(), v.a().getX() + v.b(), v.a().getY() + 4, v.a().getZ() + v.b())).size();
	}
	
	protected static int getReputationForPlayer(Village v, Player p)
	{
		return v.a(p.getName());
	}
	
	protected static boolean isPlayerReputationTooLow(Village v, Player p)
	{
		return v.d(p.getName());
	}
	
	protected static boolean isMatingSeason(Village v)
	{
		return v.i();
	}
}
