package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.meta.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class Router extends Block{
    protected float speed = 8f;

    public Router(String name){
        super(name);
        solid = true;
        update = true;
        hasItems = true;
        itemCapacity = 1;
        group = BlockGroup.transportation;
        unloadable = false;
		configurable = true;
    }

	@Override
	public void configured(Tile tile, Player player, int value){
        RouterEntity entity = tile.entity();
		entity.dirOverride ^= 1 << value;
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);
        RouterEntity entity = tile.entity();
		Draw.color(Color.valueOf("884444"));
		Lines.stroke(1.5f);
		
		int ovr = entity.dirOverride;
		
		float tsm = (1.5f-tilesize)/2;
		float tsp = (tilesize-1.5f)/2;
		
		if((ovr & 1) == 1) //right
			Lines.line(tile.drawx()+tsm,tile.drawy()+tsm,tile.drawx()+tsm,tile.drawy()+tsp);
		if((ovr & 2) == 2) //up
			Lines.line(tile.drawx()+tsm,tile.drawy()+tsm,tile.drawx()+tsp,tile.drawy()+tsm);
		if((ovr & 4) == 4) //left
			Lines.line(tile.drawx()+tsp,tile.drawy()+tsm,tile.drawx()+tsp,tile.drawy()+tsp);
		if((ovr & 8) == 8) //down
			Lines.line(tile.drawx()+tsm,tile.drawy()+tsp,tile.drawx()+tsp,tile.drawy()+tsp);
			
		Draw.color();
	}
	
	@Override
	public void buildTable(Tile tile, Table table){
		RouterEntity entity = tile.entity();
        DirSelection.buildDirOvrTable(table, tile, entity.dirOverride);
	}
	
    @Override
    public void update(Tile tile){
        RouterEntity entity = tile.entity();

        if(entity.lastItem == null && entity.items.total() > 0){
            entity.items.clear();
        }

        if(entity.lastItem != null){
            entity.time += 1f / speed * Time.delta();
            Tile target = getTileTarget(tile, entity.lastItem, entity.lastInput, false);

            if(target != null && (entity.time >= 1f || !(target.block() instanceof Router))){
                getTileTarget(tile, entity.lastItem, entity.lastInput, true);
                target.block().handleItem(entity.lastItem, target, Edges.getFacingEdge(tile, target));
                entity.items.remove(entity.lastItem, 1);
                entity.lastItem = null;
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        RouterEntity entity = tile.entity();
		
        int relative = source.relativeTo(tile.x, tile.y);

		if(relative > -1) {
			int flag = 1 << ((relative + 4) % 4);
			if((entity.dirOverride & flag) == flag) return false;
		}
		
        return tile.getTeam() == source.getTeam() && entity.lastItem == null && entity.items.total() == 0;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        RouterEntity entity = tile.entity();
        entity.items.add(item, 1);
        entity.lastItem = item;
        entity.time = 0f;
        entity.lastInput = source;
    }

    Tile getTileTarget(Tile tile, Item item, Tile from, boolean set){
        Array<Tile> proximity = tile.entity.proximity();
        int counter = tile.rotation();
        for(int i = 0; i < proximity.size; i++){
            Tile other = proximity.get((i + counter) % proximity.size);
            if(set) tile.rotation((byte)((tile.rotation() + 1) % proximity.size));
            if(other == from && from.block() == Blocks.overflowGate) continue;
            if(other.block().acceptItem(item, other, Edges.getFacingEdge(tile, other))){
                return other;
            }
        }
        return null;
    }

    @Override
    public int removeStack(Tile tile, Item item, int amount){
        RouterEntity entity = tile.entity();
        int result = super.removeStack(tile, item, amount);
        if(result != 0 && item == entity.lastItem){
            entity.lastItem = null;
        }
        return result;
    }

    @Override
    public TileEntity newEntity(){
        return new RouterEntity();
    }

    public class RouterEntity extends TileEntity{
        Item lastItem;
        Tile lastInput;
        float time;
		int dirOverride = 0;
		
		@Override
		public int config(){
			return dirOverride;
		}

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
			stream.writeShort(dirOverride);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
			dirOverride = stream.readShort();
        }
    }
}
