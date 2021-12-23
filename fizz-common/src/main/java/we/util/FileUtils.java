package we.util;

import java.io.File;
import java.util.Objects;

/**
 * @author hongqiaowei
 */

public abstract class FileUtils {

    private FileUtils() {
    }

    public static String getAppRootDir() {
        return new File(Consts.S.EMPTY).getAbsolutePath();
    }

    public static String getAbsoluteDir(Class<?> cls) {
        return new File(Objects.requireNonNull(cls.getResource(Consts.S.EMPTY)).getPath()).getAbsolutePath();
    }

    public static String getAbsolutePath(Class<?> cls) {
        String absoluteDir = getAbsoluteDir(cls);
        return absoluteDir + File.separatorChar + cls.getSimpleName() + ".class";
    }

    /**
     * @param file relative to src or resource dir, eg: we/util/FileUtils.class and application.yml
     */
    public static String getAbsolutePath(String file) {
        return new File(Objects.requireNonNull(FileUtils.class.getClassLoader().getResource(file)).getPath()).getAbsolutePath();
    }
}
