package com.alibaba.cobarclient.route;

import com.alibaba.cobarclient.IShardTable;

/**
 * 分表路由，需要自己在业务里面实现
 *
 * @author kevin
 */
public abstract class ShardTableRouter<T extends IShardTable> {
    public abstract int route(T t);

    public int routeAndSetTableId(T t) {
        Integer tableId = route(t);
        t.setTableId(tableId);
        return tableId;
    }
}
