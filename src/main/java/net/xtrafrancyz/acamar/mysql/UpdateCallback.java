package net.xtrafrancyz.acamar.mysql;

/**
 * @author xtrafrancyz
 */
public interface UpdateCallback extends Callback {
    void done(int updates) throws Exception;
}
