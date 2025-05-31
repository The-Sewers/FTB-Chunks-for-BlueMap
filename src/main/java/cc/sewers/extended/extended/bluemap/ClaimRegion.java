package cc.sewers.extended.extended.bluemap;

import com.flowpowered.math.vector.Vector2i;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClaimRegion {
    private final UUID teamId;
    private final String dimension;
    private final String teamName;
    private final int teamColor;
    private final Set<ChunkDimPos> chunks;

    public ClaimRegion(UUID teamId, String dimension, String teamName, int teamColor) {
        this.teamId = teamId;
        this.dimension = dimension;
        this.teamName = teamName;
        this.teamColor = teamColor;
        this.chunks = new HashSet<>();
    }

    public void addChunk(ChunkDimPos pos) {
        chunks.add(pos);
    }

    public Set<ChunkDimPos> getChunks() {
        return chunks;
    }

    public Vector2i[] getChunkCoordinates() {
        return chunks.stream()
                .map(pos -> new Vector2i(pos.x(), pos.z()))
                .toArray(Vector2i[]::new);
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getDimension() {
        return dimension;
    }

    public String getTeamName() {
        return teamName;
    }

    public int getTeamColor() {
        return teamColor;
    }

    public String getMarkerId() {
        return "region_" + teamId + "_" + dimension + "_" + hashCode();
    }

    public String getLabel() {
        return teamName + "'s Claim";
    }
}
