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

public class Junction extends Block{
    protected float speed = 26; //frames taken to go through this junction
    protected int capacity = 6;

    public Junction(String name){
        super(name);
        update = true;
        solid = true;
        instantTransfer = true;
        group = BlockGroup.transportation;
        unloadable = false;
		configurable = true;
		configWithInv = true;
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        return 0;
    }

    @Override
    public boolean outputsItems(){
        return true;
    }
	
	@Override
	public void configured(Tile tile, Player player, int value){
        JunctionEntity entity = tile.entity();
		entity.dirOverride ^= 1 << value;
	}

	@Override
	public void draw(Tile tile){
		super.draw(tile);
        JunctionEntity entity = tile.entity();
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
		JunctionEntity entity = tile.entity();
        DirSelection.buildDirOvrTable(table, tile, entity.dirOverride);
	}
	
    @Override
    public void update(Tile tile){
        JunctionEntity entity = tile.entity();
        DirectionalItemBuffer buffer = entity.buffer;

        for(int i = 0; i < 4; i++){
            if(buffer.indexes[i] > 0){
                if(buffer.indexes[i] > capacity) buffer.indexes[i] = capacity;
                long l = buffer.buffers[i][0];
                float time = BufferItem.time(l);

                if(Time.time() >= time + speed || Time.time() < time){

                    Item item = content.item(BufferItem.item(l));
                    Tile dest = tile.getNearby(i);
                    if(dest != null) dest = dest.link();

                    //skip blocks that don't want the item, keep waiting until they do
                    if(dest == null || !dest.block().acceptItem(item, dest, tile)){
                        continue;
                    }

                    dest.block().handleItem(item, dest, tile);
                    System.arraycopy(buffer.buffers[i], 1, buffer.buffers[i], 0, buffer.indexes[i] - 1);
                    buffer.indexes[i] --;
                }
            }
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        JunctionEntity entity = tile.entity();
        int relative = source.relativeTo(tile.x, tile.y);
        entity.buffer.accept(relative, item);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        JunctionEntity entity = tile.entity();
        int relative = source.relativeTo(tile.x, tile.y);

        if(entity == null || relative == -1 || !entity.buffer.accepts(relative))
            return false;
        Tile to = tile.getNearby(relative);
		if(to == null || to.link().entity == null) return false;
		
		if(relative > -1) {
			int flag = 1 << ((relative + 4) % 4);
			if((entity.dirOverride & flag) == flag) return false;
		}

		return true;
    }

    @Override
    public TileEntity newEntity(){
        return new JunctionEntity();
    }

    class JunctionEntity extends TileEntity{
		int dirOverride = 0;
        DirectionalItemBuffer buffer = new DirectionalItemBuffer(capacity, speed);

		@Override
		public int config(){
			return dirOverride;
		}

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
			stream.writeShort(dirOverride);
            buffer.write(stream);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
			dirOverride = stream.readShort();
            buffer.read(stream);
        }
    }
}
