/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.storage.database.queries;

import com.djrapitops.plan.gathering.domain.TPS;
import com.djrapitops.plan.gathering.domain.builders.TPSBuilder;
import com.djrapitops.plan.storage.database.sql.tables.ServerTable;
import com.djrapitops.plan.storage.database.sql.tables.TPSTable;
import com.djrapitops.plan.storage.database.sql.tables.WorldTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.djrapitops.plan.storage.database.sql.building.Sql.*;

/**
 * Static method class for queries that use large amount of memory.
 *
 * @author Rsl1122
 */
public class LargeFetchQueries {

    private LargeFetchQueries() {
        /* Static method class */
    }

    /**
     * Query database for TPS data.
     *
     * @return Map: Server UUID - List of TPS data
     */
    public static Query<Map<UUID, List<TPS>>> fetchAllTPSData() {
        String serverIDColumn = ServerTable.TABLE_NAME + '.' + ServerTable.SERVER_ID;
        String serverUUIDColumn = ServerTable.TABLE_NAME + '.' + ServerTable.SERVER_UUID + " as s_uuid";
        String sql = SELECT +
                TPSTable.DATE + ',' +
                TPSTable.TPS + ',' +
                TPSTable.PLAYERS_ONLINE + ',' +
                TPSTable.CPU_USAGE + ',' +
                TPSTable.RAM_USAGE + ',' +
                TPSTable.ENTITIES + ',' +
                TPSTable.CHUNKS + ',' +
                TPSTable.FREE_DISK + ',' +
                serverUUIDColumn +
                FROM + TPSTable.TABLE_NAME +
                INNER_JOIN + ServerTable.TABLE_NAME + " on " + serverIDColumn + "=" + TPSTable.SERVER_ID;

        return new QueryAllStatement<Map<UUID, List<TPS>>>(sql, 50000) {
            @Override
            public Map<UUID, List<TPS>> processResults(ResultSet set) throws SQLException {
                Map<UUID, List<TPS>> serverMap = new HashMap<>();
                while (set.next()) {
                    UUID serverUUID = UUID.fromString(set.getString("s_uuid"));

                    List<TPS> tpsList = serverMap.getOrDefault(serverUUID, new ArrayList<>());

                    TPS tps = TPSBuilder.get()
                            .date(set.getLong(TPSTable.DATE))
                            .tps(set.getDouble(TPSTable.TPS))
                            .playersOnline(set.getInt(TPSTable.PLAYERS_ONLINE))
                            .usedCPU(set.getDouble(TPSTable.CPU_USAGE))
                            .usedMemory(set.getLong(TPSTable.RAM_USAGE))
                            .entities(set.getInt(TPSTable.ENTITIES))
                            .chunksLoaded(set.getInt(TPSTable.CHUNKS))
                            .freeDiskSpace(set.getLong(TPSTable.FREE_DISK))
                            .toTPS();

                    tpsList.add(tps);
                    serverMap.put(serverUUID, tpsList);
                }
                return serverMap;
            }
        };
    }

    /**
     * Query database for world names.
     *
     * @return Map: Server UUID - List of world names
     */
    public static Query<Map<UUID, Collection<String>>> fetchAllWorldNames() {
        String sql = SELECT + '*' + FROM + WorldTable.TABLE_NAME;

        return new QueryAllStatement<Map<UUID, Collection<String>>>(sql, 1000) {
            @Override
            public Map<UUID, Collection<String>> processResults(ResultSet set) throws SQLException {
                Map<UUID, Collection<String>> worldMap = new HashMap<>();
                while (set.next()) {
                    UUID serverUUID = UUID.fromString(set.getString(WorldTable.SERVER_UUID));
                    Collection<String> worlds = worldMap.getOrDefault(serverUUID, new HashSet<>());
                    worlds.add(set.getString(WorldTable.NAME));
                    worldMap.put(serverUUID, worlds);
                }
                return worldMap;
            }
        };
    }
}