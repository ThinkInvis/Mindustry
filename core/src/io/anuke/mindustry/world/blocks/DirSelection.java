package io.anuke.mindustry.world.blocks;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.*;

public class DirSelection{
    public static void buildDirOvrTable(Table table, Tile tile, int currState){
        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        Table cont = new Table();
        cont.defaults().size(32);
		
		//UL: placeholder
		cont.addImage(Core.atlas.drawable("blank"), Color.valueOf("22222277"));
		//UC: block top input (down arrow / flag 8 / config 3)
		cont.addImageButton(Core.atlas.drawable("icon-arrow-down-small"), Styles.clearToggleTransi, () -> tile.configure(3)).update(b -> b.setChecked((currState & 8) == 8));
		//UR: placeholder
		cont.addImage(Core.atlas.drawable("blank"), Color.valueOf("22222277"));
		cont.row();
		//CL: block left input (right arrow / flag 1 / config 0)
		cont.addImageButton(Core.atlas.drawable("icon-arrow-right-small"), Styles.clearToggleTransi, () -> tile.configure(0)).update(b -> b.setChecked((currState & 1) == 1));
		//CC: placeholder
		cont.addImage(Core.atlas.drawable("blank"), Color.valueOf("22222277"));
		//CR: block right input (left arrow / flag 4 / config 2)
		cont.addImageButton(Core.atlas.drawable("icon-arrow-left-small"), Styles.clearToggleTransi, () -> tile.configure(2)).update(b -> b.setChecked((currState & 4) == 4));
		cont.row();
		//LL: placeholder
		cont.addImage(Core.atlas.drawable("blank"), Color.valueOf("22222277"));
		//LC: block bottom input (up arrow / flag 2 / config 1)
		cont.addImageButton(Core.atlas.drawable("icon-arrow-up-small"), Styles.clearToggleTransi, () -> tile.configure(1)).update(b -> b.setChecked((currState & 2) == 2));
		//LR: placeholder
		cont.addImage(Core.atlas.drawable("blank"), Color.valueOf("22222277"));

        table.add(cont);
		table.row();
        table.label(() -> "Blocked Inputs").style(Styles.outlineLabel).center().growX().get().setAlignment(Align.center);
    }
}
