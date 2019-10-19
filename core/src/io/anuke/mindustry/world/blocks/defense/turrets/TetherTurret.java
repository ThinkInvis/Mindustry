package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.arc.Core;
import io.anuke.arc.collection.*;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.*;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import static io.anuke.mindustry.Vars.tilesize;

public class TetherTurret extends Turret{
    protected BulletType shootType;
    protected float firingMoveFract = 0.1f;
    protected float holdRange = 80f;
	protected float pullPower = 1.5f;
	protected float stoppingPower = 0.2f;
	protected float powerUse = 0.2f;
	protected float minPowerThresh = 0.1f;
	protected float rotatePerTarget = 2f;
	
    public TetherTurret(String name){
        super(name);
		hasPower = true;
		canOverdrive = false;
		targetGround = false;
    }

    /*@Override
    public void setStats(){
        super.setStats();

    }*/
	//does not damage, cannot be boosted

	//todo: PLACEHOLDER GRAPHICS, remove this override entirely once i figure out how to add a sprite to atlas
	@Override
    public void load(){
        super.load();

        region = Core.atlas.find("lancer");
        baseRegion = Core.atlas.find("block-" + size);
        heatRegion = Core.atlas.find("lancer-heat");
    }
	
	@Override
	public void init(){
		consumes.powerCond(powerUse, entity -> ((TurretEntity)entity).target != null && ((TurretEntity)entity).target.isValid());
        super.init();
	}

    @Override
    public BulletType useAmmo(Tile tile){
        //nothing used directly
        return shootType;
    }

    @Override
    public boolean hasAmmo(Tile tile){
        //you can always rotate, but never shoot if there's no power
        return true;
    }
	
    @Override
    protected float baseReloadSpeed(Tile tile){
        return tile.isEnemyCheat() ? 1f : tile.entity.power.satisfaction;
    }
	
	@Override
    protected void findTarget(Tile tile){
        TetherTurretEntity entity = tile.entity();

		entity.targets.clear();
		entity.bullets.clear();
		
		Units.nearbyEnemies(tile.getTeam(), tile.drawx() - range, tile.drawy() - range, range*2f, range*2f, e -> {
            if(e.isDead() || !e.isFlying()) return;
			entity.targets.add(e);
			entity.bullets.add(Bullet.create(shootType, entity, tile.getTeam(), tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation));
        });
    }
	
	@Override
	protected boolean validateTarget(Tile tile){
		TetherTurretEntity entity = tile.entity();
		entity.targets = entity.targets.select(tgt -> !Units.invalidateTarget(tgt, tile.getTeam(), tile.drawx(), tile.drawy()));
		return !entity.targets.isEmpty();
	}
	
    @Override
    public void update(Tile tile){
        TetherTurretEntity entity = tile.entity();
		
        entity.recoil = Mathf.lerpDelta(entity.recoil, 0f, restitution);
        entity.heat = Mathf.lerpDelta(entity.heat, 0f, cooldown);
		
		if(baseReloadSpeed(tile) < minPowerThresh) {
			entity.targets.clear();
			entity.bullets.clear();
		} else if(entity.timer.get(timerTarget, targetInterval)){
			findTarget(tile);
		}
			
		if(validateTarget(tile)){
			for(int i = 0; i < entity.targets.size; i++) {
				TargetTrait t = entity.targets.get(i);
				Bullet b = entity.bullets.get(i);
				
				b.rot((new Vector2(t.getX()-tile.drawx(),t.getY()-tile.drawy())).angle());
				b.set(tile.drawx(),tile.drawy());
				b.time(0f);
				
				pushTarget(tile, t);
			}
			entity.rotation += entity.targets.size * rotatePerTarget * Time.delta();
			//entity.recoil = 0f;
			entity.heat = 1f;
			tr.trns(entity.rotation, size * tilesize / 2f, 0f);
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
		
		float pfac = baseReloadSpeed(tile) / entity.targets.size;
		//float sfac = -Mathf.clamp(1 - stoppingPower * pfac * Time.delta(), 0f, 1f);
		//entity.target.applyImpulse(entity.target.getTargetVelocityX()*sfac, entity.target.getTargetVelocityY()*sfac);
		tgt.applyImpulse(didx * pullPower * pfac * Time.delta(), didy * pullPower * pfac * Time.delta());
		
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

    class TetherTurretEntity extends TurretEntity{
		public Array<TargetTrait> targets = new Array<TargetTrait>();
        public Array<Bullet> bullets = new Array<Bullet>();
    }
}
