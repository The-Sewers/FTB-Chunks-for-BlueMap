package cc.sewers.extended.extended.ftbchunks;

import com.flowpowered.math.vector.Vector2i;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.UsernameCache;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ClaimRegion {
    private final Team team;
    private final String dimension;
    private final String teamName;
    private final int teamColor;
    private final Set<ChunkDimPos> chunks;
    private static final Map<UUID, AtomicInteger> teamRegionCounters = new ConcurrentHashMap<>();
    private final int regionNumber;

    public ClaimRegion(Team team, String dimension, String teamName, int teamColor) {
        this.team = team;
        this.dimension = dimension;
        this.teamName = teamName;
        this.teamColor = teamColor;
        this.chunks = new HashSet<>();
        this.regionNumber = teamRegionCounters
            .computeIfAbsent(team.getTeamId(), k -> new AtomicInteger(0))
            .incrementAndGet();
    }

    public void addChunk(ChunkDimPos pos) {
        chunks.add(pos);
    }

    public Vector2i[] getChunkCoordinates() {
        return chunks.stream()
                .map(pos -> new Vector2i(pos.x(), pos.z()))
                .toArray(Vector2i[]::new);
    }

    public UUID getTeamId() {
        return team.getTeamId();
    }

    public String getDimension() {
        return dimension;
    }

    public int getTeamColor() {
        return teamColor;
    }

    public String getMarkerId() {
        return "region_" + getTeamId() + "_" + dimension + "_" + hashCode();
    }

    public String getLabel() {
        double avgX = 0;
        double avgZ = 0;

        if (!chunks.isEmpty()) {
            for (ChunkDimPos pos : chunks) {
                avgX += pos.x() * 16;
                avgZ += pos.z() * 16;
            }
            avgX /= chunks.size();
            avgZ /= chunks.size();
        }

        int centerX = (int) avgX;
        int centerZ = (int) avgZ;

        return teamName + "'s Region " + regionNumber + " (" + centerX + " | ~ | " + centerZ + ")";
    }

    public static void resetRegionCounters() {
        teamRegionCounters.clear();
    }

    private record MemberDisplayInfo(String displayName, boolean isOnline,
                                     UUID id) implements Comparable<MemberDisplayInfo> {

        @Override
            public int compareTo(@NotNull MemberDisplayInfo other) {
                if (this.isOnline && !other.isOnline) {
                    return -1;
                }
                if (!this.isOnline && other.isOnline) {
                    return 1;
                }
                return this.displayName.compareToIgnoreCase(other.displayName);
            }

            public String toHtml() {
                String statusColor = "#7CFC00";
                return escapeHtml(displayName) +
                        (isOnline
                                ? " <font color='" + statusColor + "'>(Online)</font>"
                                : "");
            }

            private static String escapeHtml(String text) {
                if (text == null)
                    return "";
                return text.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#39;");
            }
        }

    public String getDetail() {
        StringBuilder htmlDetail = new StringBuilder();

        htmlDetail.append("<strong>").append(MemberDisplayInfo.escapeHtml(this.teamName)).append("</strong><br>")
                .append("<br><br>");

        Set<UUID> allMemberIds = this.team.getMembers();
        Map<UUID, String> onlineMembers = new HashMap<>();

        for (ServerPlayer onlinePlayer : this.team.getOnlineMembers()) {
            onlineMembers.put(onlinePlayer.getUUID(), onlinePlayer.getGameProfile().getName());
        }

        if (allMemberIds.isEmpty()) {
            htmlDetail.append("<em>No members</em>");
        } else {
            htmlDetail
                    .append("<strong>Members (")
                    .append(allMemberIds.size())
                    .append("):</strong><br>");
            List<MemberDisplayInfo> memberInfos = new ArrayList<>();

            for (UUID memberId : allMemberIds) {
                String nameToDisplay;
                if (onlineMembers.containsKey(memberId)) {
                    nameToDisplay = onlineMembers.get(memberId);
                } else {
                    nameToDisplay = UsernameCache.getLastKnownUsername(memberId);
                }

                boolean isOnline = onlineMembers.containsKey(memberId);
                memberInfos.add(
                        new MemberDisplayInfo(nameToDisplay, isOnline, memberId)
                );
            }

            Collections.sort(memberInfos);

            for (MemberDisplayInfo info : memberInfos) {
                htmlDetail.append("- ").append(info.toHtml()).append("<br>");
            }

            if (!memberInfos.isEmpty() && htmlDetail.length() >= "<br>".length()) {
                htmlDetail.setLength(htmlDetail.length() - "<br>".length());
            }
        }

        return htmlDetail.toString();
    }
}
