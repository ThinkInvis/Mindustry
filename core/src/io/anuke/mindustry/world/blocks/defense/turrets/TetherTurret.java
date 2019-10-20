package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.arc.Core;
import io.anuke.arc.audio.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.Blending;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Effects.Effect;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.*;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.tilesize;

public class TetherTurret extends Block{
    protected static final int targetInterval = 20;
	
    protected final int timerTarget = timers++;
	
    protected float range = 50f;
	
    protected float holdRange = 80f;
	protected float pullPower = 1.5f;
	protected float stoppingPower = 0.2f;
	protected float powerUse = 0.2f;
	protected float minPowerThresh = 0.1f;
	protected float rotatePerTarget = 2f;
    protected boolean targetAir = true;
    protected boolean targetGround = true;

    protected TextureRegion baseRegion, laser, laserEnd;
	
    public TetherTurret(String name){
        super(name);
        priority = TargetPriority.turret;
		update = true;
		solid = true;
        layer = Layer.turret;
        layer2 = Layer.power;
        group = BlockGroup.turrets;
        flags = EnumSet.of(BlockFlag.turret);
        outlineIcon = true;
		hasPower = true;
		canOverdrive = false;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }


    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.shootRange, range / tilesize, StatUnit.blocks);
        stats.add(BlockStat.targetsAir, targetAir);
        stats.add(BlockStat.targetsGround, targetGround);
    }

	//todo: PLACEHOLDER GRAPHICS, figure out how to add a sprite to atlas
	@Override
    public void load(){
        super.load();

        baseRegion = Core.atlas.find("block-" + size);
        laser = Core.atlas.find("laser");
        laserEnd = Core.atlas.find("laser-end");
    }
	
	@Override
	public void init(){
		consumes.powerCond(powerUse, entity -> !((TetherTurretEntity)entity).targets.isEmpty()); //TODO: increase power use per target instead of decreasing effects for multiple targets?
        super.init();
	}
	
    @Override
    public void draw(Tile tile){
        Draw.rect(baseRegion, tile.drawx(), tile.drawy());
    }

    @Override
    public void drawLayer(Tile tile){
        TetherTurretEntity entity = tile.entity();

        Draw.rect(region, tile.drawx(), tile.drawy(), entity.rotation - 90);
    }

    @Override
    public void drawLayer2(Tile tile){
        TetherTurretEntity entity = tile.entity();

        if(!entity.targets.isEmpty()){
			for(int i = 0; i < entity.targets.size; i++) {
				TargetTrait t = entity.targets.get(i);
				
				Draw.color(Color.valueOf("d7e8ff"));
				Drawf.laser(laser, laserEnd,
					tile.drawx(), tile.drawy(),
					t.getX(), t.getY(),
					0.25f/entity.targets.size);
			}
			entity.rotation += entity.targets.size * rotatePerTarget * Time.delta();
        }
    }
	
    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find("lancer-base"), Core.atlas.find("lancer")};
    }
	
    @Override
    public void drawSelect(Tile tile){
        Drawf.dashCircle(tile.drawx(), tile.drawy(), range, tile.getTeam().color);
    }
	
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset(), y * tilesize + offset(), range, Pal.placing);
    }
	
    protected float pullPowerMod(Tile tile){
        return tile.isEnemyCheat() ? 1f : tile.entity.power.satisfaction;
    }
	
    protected void findTarget(Tile tile){
        TetherTurretEntity entity = tile.entity();

		entity.targets.clear();
		
		Units.nearbyEnemies(tile.getTeam(), tile.drawx() - range, tile.drawy() - range, range*2f, range*2f, e -> {
            if(e.isDead() || (e.isFlying() && !targetAir) || (!e.isFlying() && !targetGround)) return; // || !e.isFlying()
			entity.targets.add(e);
        });
    }
	
	protected boolean validateTarget(Tile tile){
		TetherTurretEntity entity = tile.entity();
		entity.targets = entity.targets.select(tgt -> !Units.invalidateTarget(tgt, tile.getTeam(), tile.drawx(), tile.drawy()));
		return !entity.targets.isEmpty();
	}
	
    @Override
    public void update(Tile tile){
        TetherTurretEntity entity = tile.entity();
		
		if(pullPowerMod(tile) < minPowerThresh) {
			entity.targets.clear();
		} else if(entity.timer.get(timerTarget, targetInterval)){
			findTarget(tile);
		}
			
		if(validateTarget(tile)){
			for(int i = 0; i < entity.targets.size; i++) {
				TargetTrait t = entity.targets.get(i);
				pushTarget(tile, t);
			}
		}
    }
	
    protected void pushTarget(Tile tile, TargetTrait tgt){
        TetherTurretEntity entity = tile.entity();
		
		float srcx = tile.drawx();
		float srcy = tile.drawy();
		float tgtx = tgt.getX();
		float tgty = tgt.getY();
		
		float tdx = tgtx - srcx;
		float tdy = tgty - srcy;
		float tdist = Mathf.sqrt(tdx*tdx+tdy*tdy);
		float ttheta = Mathf.atan2(tdx, tdy);
		
		float idealx = srcx + holdRange * Mathf.cos(ttheta);
		float idealy = srcy + holdRange * Mathf.sin(ttheta);
		
		float didx = idealx - tgtx;
		float didy = idealy - tgty;
		float didr = Mathf.sqrt(didx*didx+didy*didy);
		if(didr > 1) {
			didx /= didr;
			didy /= didr;
		}
		
		float pfac = pullPowerMod(tile) / entity.targets.size;
		//float sfac = -Mathf.clamp(1 - stoppingPower * pfac * Time.delta(), 0f, 1f);
		//entity.target.applyImpulse(entity.target.getTargetVelocityX()*sfac, entity.target.getTargetVelocityY()*sfac);
		tgt.applyImpulse(didx * pullPower * pfac * Time.delta(), didy * pullPower * pfac * Time.delta());
    }

    @Override
    public boolean shouldConsume(Tile tile){
        TetherTurretEntity entity = tile.entity();
        return !entity.targets.isEmpty();
    }

    @Override
    public TileEntity newEntity(){
        return new TetherTurretEntity();
    }

    @Override
    public boolean shouldActiveSound(Tile tile){
        TetherTurretEntity entity = tile.entity();

        return !entity.targets.isEmpty();
    }

    protected boolean isTurret(Tile tile){
        return (tile.entity instanceof TetherTurretEntity);
    }

    class TetherTurretEntity extends TileEntity{
		public float rotation = 90;
		public Array<TargetTrait> targets = new Array<TargetTrait>();
    }
}
