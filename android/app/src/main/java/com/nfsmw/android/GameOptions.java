package com.nfsmw.android;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * The graphics options of the launcher. Each one is a game cvar: GameActivity passes them on the command
 * line, which wins over nfsmw.toml. Values must be ones the cvar allows (nfsmw_ajustes_graficos.cpp and
 * friends), or the game ignores them.
 */
final class GameOptions {
    static final class Option {
        final String key;
        final int titleRes;
        final String cvar;
        final String[] values;
        final int[] labelRes;
        final String defaultValue;

        Option(String key, int titleRes, String cvar, String defaultValue, String[] values, int[] labelRes) {
            this.key = key;
            this.titleRes = titleRes;
            this.cvar = cvar;
            this.defaultValue = defaultValue;
            this.values = values;
            this.labelRes = labelRes;
        }

        String title(Context context) {
            return context.getString(titleRes);
        }

        String label(Context context, String value) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(value)) {
                    return context.getString(labelRes[i]);
                }
            }
            return value;
        }

        String[] labels(Context context) {
            String[] labels = new String[labelRes.length];
            for (int i = 0; i < labelRes.length; i++) {
                labels[i] = context.getString(labelRes[i]);
            }
            return labels;
        }
    }

    static final String RESOLUTION = "resolution";
    static final String FPS = "fps";
    static final String RENDERER = "renderer";

    static final Option[] ALL = {
            new Option(RENDERER, R.string.opt_renderer, "nfsmw_renderizador", "nativo",
                    new String[] {"nativo", "xenos"},
                    new int[] {R.string.opt_renderer_native, R.string.opt_renderer_compat}),
            new Option(RESOLUTION, R.string.opt_resolution, "nfsmw_resolucion_interna", "1280x720",
                    new String[] {"640x360", "1024x576", "1280x720", "1920x1080"},
                    new int[] {R.string.opt_resolution_640, R.string.opt_resolution_1024, R.string.opt_resolution_1280,
                            R.string.opt_resolution_1920}),
            new Option(FPS, R.string.opt_fps, "nfsmw_limite_fps", "60",
                    new String[] {"30", "60", "90", "120"},
                    new int[] {R.string.opt_fps_30, R.string.opt_fps_60, R.string.opt_fps_90, R.string.opt_fps_120}),
            new Option("aa", R.string.opt_antialiasing, "nfsmw_antialiasing", "apagado",
                    new String[] {"apagado", "fxaa"},
                    new int[] {R.string.opt_antialiasing_off, R.string.opt_antialiasing_fxaa}),
            new Option("shadows", R.string.opt_shadows, "nfsmw_sombras_cada", "1",
                    new String[] {"1", "2"},
                    new int[] {R.string.opt_shadows_every_frame, R.string.opt_shadows_every_2}),
            new Option("car_reflections", R.string.opt_car_reflections, "nfsmw_cubemap_caras_max", "6",
                    new String[] {"6", "2", "1"},
                    new int[] {R.string.opt_car_reflections_high, R.string.opt_car_reflections_medium,
                            R.string.opt_car_reflections_low}),
            new Option("road_reflection", R.string.opt_road_reflection, "nfsmw_reflejo_carretera", "true",
                    new String[] {"true", "false"},
                    new int[] {R.string.opt_road_reflection_on, R.string.opt_road_reflection_off}),
            new Option("sky", R.string.opt_sky, "nfsmw_resplandor_cielo", "natural",
                    new String[] {"original", "natural", "suave"},
                    new int[] {R.string.opt_sky_original, R.string.opt_sky_natural, R.string.opt_sky_soft}),
            new Option("volume", R.string.opt_volume, "audio_ganancia_pct", "100",
                    new String[] {"100", "125", "150", "200"},
                    new int[] {R.string.opt_volume_normal, R.string.opt_volume_high, R.string.opt_volume_very_high,
                            R.string.opt_volume_max}),
            new Option("filter", R.string.opt_filter, "nfsmw_posproceso", "apagado",
                    new String[] {"apagado", "cine", "vivo", "calido", "frio", "sepia", "noir", "crt"},
                    new int[] {R.string.opt_filter_none, R.string.opt_filter_cinema, R.string.opt_filter_vivid,
                            R.string.opt_filter_warm, R.string.opt_filter_cold, R.string.opt_filter_sepia,
                            R.string.opt_filter_noir, R.string.opt_filter_crt}),
    };

    private static final String PREFS = "nfsmw_game";

    private GameOptions() {
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static Option find(String key) {
        for (Option o : ALL) {
            if (o.key.equals(key)) {
                return o;
            }
        }
        throw new IllegalArgumentException(key);
    }

    static String get(Context context, String key) {
        Option o = find(key);
        String value = prefs(context).getString(key, o.defaultValue);
        for (String allowed : o.values) {
            if (allowed.equals(value)) {
                return value;
            }
        }
        return o.defaultValue;
    }

    static void set(Context context, String key, String value) {
        prefs(context).edit().putString(key, value).apply();
    }

    static List<String> arguments(Context context) {
        List<String> args = new ArrayList<>();
        for (Option o : ALL) {
            args.add("--" + o.cvar + "=" + get(context, o.key));
        }
        if ("xenos".equals(get(context, RENDERER))) {
            // Use ordinary descriptor sets and host framebuffers on older drivers.
            // Xenos also decompresses unsupported BC texture formats on the GPU.
            args.add("--vulkan_native_shader_features=false");
            args.add("--render_target_path_vulkan=fbo");
            args.add("--vulkan_require_geometry_shader=false");
            args.add("--vulkan_require_fill_mode_non_solid=false");
            args.add("--async_shader_compilation=false");
            args.add("--nfsmw_d3d_registros_nativo=false");
            args.add("--nfsmw_d3d_marcador=false");
            args.add("--nfsmw_d3d_marcador_registro=false");
            args.add("--nfsmw_d3d_efectos_nativo=false");
            args.add("--nfsmw_render_sin_mosaico=false");
            args.add("--nfsmw_material_nativo=false");
            args.add("--nfsmw_visible_nativo=false");
            args.add("--nfsmw_matrices_nativo=false");
            args.add("--nfsmw_eview_nativo=false");
            args.add("--nfsmw_escenario_nativo=false");
            args.add("--nfsmw_efecto_pasada_nativo=false");
            args.add("--nfsmw_pegamento_nativo=false");
        }
        return args;
    }
}
