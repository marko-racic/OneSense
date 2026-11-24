package rs.racic.onesense;

/** Predefinisane palete za widžete: (pozadina, broj, sporedni tekst). */
public class Palette {

    public static final int[][] COLORS = {
            {0xFF1A2029, 0xFFFF7A5C, 0xFF8A97A6}, // tamna (podrazumevana)
            {0xFF123528, 0xFF6EE7B7, 0xFF9FD8C4}, // zelena
            {0xFF14273D, 0xFF7CC7F7, 0xFF9FBDD6}, // plava
            {0xFF2A1F3D, 0xFFC9A6F7, 0xFFB3A6C9}, // ljubicasta
            {0xFF3D2A12, 0xFFF7B267, 0xFFD6BC9F}, // narandzasta
            {0xFF3D1F24, 0xFFF78CA0, 0xFFD6A0AA}, // crvena
            {0xFFEEF2F6, 0xFFB8452A, 0xFF5A6B7A}, // svetla
    };

    public static int bg(int i) { return COLORS[idx(i)][0]; }
    public static int num(int i) { return COLORS[idx(i)][1]; }
    public static int sub(int i) { return COLORS[idx(i)][2]; }

    public static int idx(int i) { return (i >= 0 && i < COLORS.length) ? i : 0; }
}
