package cl.netgamer.villageinfo;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;

class BoxScanner
{
	/*
	+--------------+
	|    ()  ()    |
	|              |
	|()          ()|
	|      ><      |
	|()          ()|
	|              |
	|    ()  ()    |
	+--------------+
	*/
	
	private Location center;
	private int xRadius = 32;
	private int yRadius = 16;
	private int zRadius = 32;
	private Set<Location> box = new HashSet<Location>();
	
	
	BoxScanner(Location center)
	{
		this.center = center;
	}
	
	
	Set<Location> scan()
	{
		for (int x = 0; x <= xRadius; ++x)
			for (int z = x; z <= zRadius; ++z)
			{
				scanCorners(x, z);
				if ( x != z )
					scanCorners(z, x);
			}
		return box;
	}
	
	
	private void scanCorners(int x, int z)
	{
		scanColumn(x, z);
		if ( x != 0 )
			scanColumn(-x, z);
		if ( z != 0 )
		{
			scanColumn(x, -z);
			if ( x != 0 )
				scanColumn(-x, -z);
		}
	}
	
	
	private void scanColumn(int x, int z)
	{
		box.add(center.clone().add(x, 0, z));
		for (int y = 1; y <= yRadius; ++y)
		{
			box.add(center.clone().add(x, y, z));
			box.add(center.clone().add(x, -y, z));
		}
	}
	
}
