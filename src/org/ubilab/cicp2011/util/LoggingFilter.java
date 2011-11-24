package org.ubilab.cicp2011.util;

import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Level;

/**
 * CICPプロジェクト用ログフィルタ
 * @author atsushi-o
 * @since 2011/11/25
 */
public class LoggingFilter implements Filter {
    @Override
    public boolean isLoggable(LogRecord record) {
        String className = record.getSourceClassName();
        if (className.startsWith("org.ubilab.cicp2011")) {
            return true;
        } else {
            if (record.getLevel().intValue() < Level.WARNING.intValue())
                return false;
            else
                return true;
        }
    }
}
