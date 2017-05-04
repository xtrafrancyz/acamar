package net.xtrafrancyz.acamar.mysql;

import java.sql.ResultSet;

/**
 * @author xtrafrancyz
 */
public interface SelectCallback extends Callback {
    void done(ResultSet rs) throws Exception;
}
