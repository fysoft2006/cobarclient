/**
 * Copyright 1999-2011 Alibaba Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cobarclient;

import com.ibatis.sqlmap.client.SqlMapExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.ibatis.SqlMapClientCallback;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.Collection;

/**
 * A DAO base class definition which adds more helper methods on batch operations.<br> Users can configure their DAO
 * implementations with same configuration items of {@link MysdalSqlMapClientTemplate}.<br> <br> Feature requested by
 * Wangyi
 *
 * @author fujohnwang, update by Wangyi
 * @since 1.0
 */
public class MysdalCobarSqlMapClientDaoSupport extends SqlMapClientDaoSupport {
    @Autowired
    private SqlMapClientTemplate sqlMapClientTemplate = new SqlMapClientTemplate();

    @PostConstruct
    public void init() {
        setSqlMapClientTemplate(sqlMapClientTemplate);
    }

    public int batchInsert(final String statementName, final Collection<?> entities)
            throws DataAccessException {
        if (isPartitionBehaviorEnabled(statementName)) {
            int counter = 0;
            DataAccessException lastEx = null;
            for (Object parameterObject : entities) {
                try {
                    getSqlMapClientTemplate().insert(statementName, parameterObject);
                    counter++;
                } catch (DataAccessException e) {
                    lastEx = e;
                }
            }
            if (lastEx != null) {
                throw lastEx;
            }
            return counter;
        } else {
            return (Integer) getSqlMapClientTemplate().execute(new SqlMapClientCallback() {
                public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                    executor.startBatch();
                    for (Object item : entities) {
                        executor.insert(statementName, item);
                    }
                    return executor.executeBatch();
                }
            });
        }
    }

    public int batchDelete(final String statementName, final Collection<?> entities)
            throws DataAccessException {
        if (isPartitionBehaviorEnabled(statementName)) {
            int counter = 0;
            DataAccessException lastEx = null;
            for (Object entity : entities) {
                try {
                    counter += getSqlMapClientTemplate().delete(statementName, entity);
                } catch (DataAccessException e) {
                    lastEx = e;
                }
            }
            if (lastEx != null) {
                throw lastEx;
            }
            return counter;
        } else {
            return (Integer) getSqlMapClientTemplate().execute(new SqlMapClientCallback() {
                public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                    executor.startBatch();
                    for (Object parameterObject : entities) {
                        executor.delete(statementName, parameterObject);
                    }
                    return executor.executeBatch();
                }
            });
        }
    }

    public int batchUpdate(final String statementName, final Collection<?> entities)
            throws DataAccessException {
        if (isPartitionBehaviorEnabled(statementName)) {
            int counter = 0;
            DataAccessException lastEx = null;
            for (Object parameterObject : entities) {
                try {
                    counter += getSqlMapClientTemplate().update(statementName, parameterObject);
                } catch (DataAccessException e) {
                    lastEx = e;
                }
            }
            if (lastEx != null) {
                throw lastEx;
            }
            return counter;
        } else {
            return (Integer) getSqlMapClientTemplate().execute(new SqlMapClientCallback() {

                public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                    executor.startBatch();
                    for (Object parameterObject : entities) {
                        executor.update(statementName, parameterObject);
                    }
                    return executor.executeBatch();
                }
            });
        }
    }

    protected boolean isPartitionBehaviorEnabled(String statementName) {

        if (getSqlMapClientTemplate() instanceof MysdalSqlMapClientTemplate) {
            return ((MysdalSqlMapClientTemplate) getSqlMapClientTemplate())
                    .isHasShard(statementName);
        }

        //在router里对每一次insert都有判断。这里先暂时全部走true。
        //TODO 性能不好，因为每一条都要去校验，后面再做一种只校验一次后面全部不校验的作法
        return true;
    }
}
