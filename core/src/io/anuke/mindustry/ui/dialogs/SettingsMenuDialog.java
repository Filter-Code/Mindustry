package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.SettingsDialog;
import io.anuke.ucore.scene.ui.SettingsDialog.SettingsTable.Setting;
import io.anuke.ucore.scene.ui.Slider;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.scene.ui.layout.Cell;

import java.util.HashMap;
import java.util.Map;

import static io.anuke.mindustry.Vars.*;

public class SettingsMenuDialog extends SettingsDialog{
    public SettingsTable graphics;
    public SettingsTable game;
    public SettingsTable sound;
    public SettingsTable cheats;

    private Table prefs;
    private Table menu;
    private boolean wasPaused;

    public SettingsMenuDialog(){
        setStyle(Core.skin.get("dialog", WindowStyle.class));

        hidden(() -> {
            if(!state.is(State.menu)){
                if(!wasPaused || Net.active())
                    state.set(State.playing);
            }
        });

        shown(() -> {
            if(!state.is(State.menu)){
                wasPaused = state.is(State.paused);
                if(ui.paused.getScene() != null){
                    wasPaused = ui.paused.wasPaused;
                }
                if(!Net.active()) state.set(State.paused);
                ui.paused.hide();
            }
        });

        setFillParent(true);
        title().setAlignment(Align.center);
        getTitleTable().row();
        getTitleTable().add(new Image("white"))
                .growX().height(3f).pad(4f).get().setColor(Palette.accent);

        content().clearChildren();
        content().remove();
        buttons().remove();

        menu = new Table();

        Consumer<SettingsTable> s = table -> {
            table.row();
            table.addImageTextButton("$text.back", "icon-arrow-left", 10 * 3, this::back).size(240f, 60f).colspan(2).padTop(15f);
        };

        game = new SettingsTable(s);
        graphics = new SettingsTable(s);
        sound = new SettingsTable(s);
        cheats = new SettingsTable(s);
        cheats.visible(() -> Settings.getBool("enable-cheats", false));

        prefs = new Table();
        prefs.top();
        prefs.margin(14f);

        menu.defaults().size(300f, 60f).pad(3f);
        menu.addButton("$text.settings.game", () -> visible(0));
        menu.row();
        menu.addButton("$text.settings.graphics", () -> visible(1));
        menu.row();
        menu.addButton("$text.settings.sound", () -> visible(2));
        if(!Vars.mobile){
            menu.row();
            menu.addButton("$text.settings.controls", ui.controls::show);
        }
        menu.row();
        menu.addButton("$text.settings.language", ui.language::show);
        menu.row();
        Cell cheatsButton = menu.addButton("$text.settings.cheats", () -> visible(3));
        cheatsButton.visible(() -> Settings.getBool("enable-cheats", false));

        prefs.clearChildren();
        prefs.add(menu);

        ScrollPane pane = new ScrollPane(prefs, "clear");
        pane.addCaptureListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button){
                Element actor = pane.hit(x, y, true);
                if(actor instanceof Slider){
                    pane.setFlickScroll(false);
                    return true;
                }

                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button){
                pane.setFlickScroll(true);
                super.touchUp(event, x, y, pointer, button);
            }
        });
        pane.setFadeScrollBars(false);

        row();
        add(pane).grow().top();
        row();
        add(buttons()).fillX();

        hidden(this::back);

        addSettings();
    }

    void addSettings(){
        sound.volumePrefs();
        game.screenshakePref();
        //game.checkPref("smoothcam", true);
        //targeted
        game.checkPref("showHP", false);
        game.sliderPref("HPBoost",1100, 1100, 10000, i -> Bundles.format("setting.health", i));

        game.checkPref("effects", true);
        if(mobile){
            game.checkPref("autotarget", true);
        }
        //game.sliderPref("sensitivity", 100, 10, 300, i -> i + "%");

        game.sliderPref("saveinterval", 60, 10, 5 * 120, i -> Bundles.format("setting.seconds", i));
        
        game.sliderPref("capacitySlider1", 2000, 100, 4000, i -> i+"");
        game.sliderPref("capacitySlider2", 20, 10, 40, i -> i+"");
        game.sliderPref("capacitySlider3", 1000, 100, 2000, i -> i+"");

        game.pref(new Setting(){
            @Override
            public void add(SettingsTable table){
                table.addButton("$text.settings.cleardata", () -> {
                    FloatingDialog dialog = new FloatingDialog("$text.settings.cleardata");
                    dialog.setFillParent(false);
                    dialog.content().defaults().size(230f, 60f).pad(3);
                    dialog.addCloseButton();
                    dialog.content().addButton("$text.settings.clearsectors", "clear", () -> {
                        ui.showConfirm("$text.confirm", "$text.settings.clear.confirm", () -> {
                            world.sectors.clear();
                            dialog.hide();
                        });
                    });
                    dialog.content().row();
                    dialog.content().addButton("$text.settings.clearunlocks", "clear", () -> {
                        ui.showConfirm("$text.confirm", "$text.settings.clear.confirm", () -> {
                            control.unlocks.reset();
                            dialog.hide();
                        });
                    });
                    dialog.content().row();
                    dialog.content().addButton("$text.settings.clearall", "clear", () -> {
                        ui.showConfirm("$text.confirm", "$text.settings.clearall.confirm", () -> {
                            Map<String, Object> map = new HashMap<>();
                            for(String value : Settings.prefs().get().keySet()){
                                if(value.contains("usid") || value.contains("uuid")){
                                    map.put(value, Settings.prefs().getString(value));
                                }
                            }
                            Settings.prefs().clear();
                            Settings.prefs().put(map);
                            Settings.save();

                            for(FileHandle file : dataDirectory.list()){
                                file.deleteDirectory();
                            }

                            Gdx.app.exit();
                        });
                    });
                    dialog.content().row();
                    dialog.show();
                }).size(220f, 60f).pad(6).left();
                table.add();
                table.row();
            }
        });

        game.checkPref("enable-cheats", false);

        graphics.sliderPref("fpscap", 125, 5, 125, 5, s -> (s > 120 ? Bundles.get("setting.fpscap.none") : Bundles.format("setting.fpscap.text", s)));
        graphics.sliderPref("redvalue",255,0,255,1, s -> (Bundles.format("setting.redvalue.text",s)));//red value slider
        graphics.sliderPref("greenvalue",255,0,255,1, s -> (Bundles.format("setting.greenvalue.text",s)));//green value slider
        graphics.sliderPref("bluevalue",255,0,255,1, s -> ( Bundles.format("setting.bluevalue.text",s)));//blue value slider
        graphics.checkPref("fog",true);//by default select fog of war
        graphics.checkPref("multithread", mobile, threads::setEnabled);

        if(Settings.getBool("multithread")){
            threads.setEnabled(true);
        }


        if(!mobile){
            graphics.checkPref("vsync", true, b -> Gdx.graphics.setVSync(b));
            graphics.checkPref("fullscreen", false, b -> {
                if(b){
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                }else{
                    Gdx.graphics.setWindowedMode(600, 480);
                }
            });

            Gdx.graphics.setVSync(Settings.getBool("vsync"));
            if(Settings.getBool("fullscreen")){
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
        }

        graphics.checkPref("fps", false);
        graphics.checkPref("lasers", true);
        graphics.checkPref("minimap", !mobile); //minimap is disabled by default on mobile devices

        cheats.sliderPref("distribution-speed", 1, 1, 5, 1, 
                          x -> (x == 1 ? Bundles.get("setting.distribution-speed.none") : Bundles.format("setting.distribution-speed.text", x)
                        ));
        cheats.sliderPref("crafting-speed", 1, 1, 5, 1, 
                          y -> (y == 1 ? Bundles.get("setting.crafting-speed.none") : Bundles.format("setting.crafting-speed.text", y)
                        ));
        cheats.sliderPref("production-speed", 1, 1, 5, 1, 
                          z -> (z == 1 ? Bundles.get("setting.production-speed.none") : Bundles.format("setting.production-speed.text", z)
                        ));
        cheats.sliderPref("power-amount", 1, 1, 5, 1, 
                          z -> (z == 1 ? Bundles.get("setting.power-amount.none") : Bundles.format("setting.power-amount.text", z)
                        ));
        cheats.sliderPref("movement-speed", 1, 1, 5, 1, 
                        z -> (z == 1 ? Bundles.get("setting.movement-speed.none") : Bundles.format("setting.movement-speed.text", z)
                        ));
    }

    private void back(){
        prefs.clearChildren();
        prefs.add(menu);
    }

    private void visible(int index){
        prefs.clearChildren();
        Table table = Mathf.select(index, game, graphics, sound, cheats);
        prefs.add(table);
    }

    @Override
    public void addCloseButton(){
        buttons().addImageTextButton("$text.menu", "icon-arrow-left", 30f, this::hide).size(230f, 64f);

        keyDown(key -> {
            if(key == Keys.ESCAPE || key == Keys.BACK)
                hide();
        });
    }
}
