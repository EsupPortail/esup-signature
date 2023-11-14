package org.esupportail.esupsignature.dss.config;

import org.springframework.web.multipart.support.StandardServletMultipartResolver;

public class MultipartResolverProvider {

    /**
     * Singleton instance
     */
    private static MultipartResolverProvider singleton;

    /**
     * Defines the maximum file upload size
     *
     * Default : 50 MB
     */
    private long maxFileSize = 52428800;

    /**
     * Defines the maximum inMemory file size
     *
     * Default : 50 MB
     */
    private int maxInMemorySize = 52428800;

    /**
     * The empty instance of multipart resolver
     */
    private StandardServletMultipartResolver emptyMultipartResolver;

    /**
     * The used MultipartResolverInstance
     */
    private StandardServletMultipartResolver commonMultipartResolver;

    /**
     * Default constructor
     */
    private MultipartResolverProvider() {
    }

    /**
     * Gets the current {@code MultipartResolverProvider} instance
     *
     * @return {@link MultipartResolverProvider}
     */
    public static MultipartResolverProvider getInstance() {
        if (singleton == null) {
            singleton = new MultipartResolverProvider();
        }
        return singleton;
    }

    /**
     * Gets max upload file size
     *
     * @return max upload file size
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Sets maximum upload file size
     *
     * @param maxFileSize maximum file size
     */
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    /**
     * Sets maximum inMemory file size
     *
     * @param maxInMemorySize maximum in memory file size
     */
    public void setMaxInMemorySize(int maxInMemorySize) {
        this.maxInMemorySize = maxInMemorySize;
    }

    /**
     * Returns an empty MultipartResolver, that does not verify the file size
     *
     * @return {@link CommonsMultipartResolver}
     */
    public StandardServletMultipartResolver getAcceptAllFilesResolver() {
        if (emptyMultipartResolver == null) {
            emptyMultipartResolver = new StandardServletMultipartResolver();
            emptyMultipartResolver.setResolveLazily(true);
        }
        return emptyMultipartResolver;
    }

    /**
     * Returns the singleton resolver instance
     *
     * @return {@link CommonsMultipartResolver}
     */
    public StandardServletMultipartResolver getCommonMultipartResolver() {
        if (commonMultipartResolver == null) {
            commonMultipartResolver = new StandardServletMultipartResolver();
            commonMultipartResolver.setResolveLazily(true);
        }
        return commonMultipartResolver;
    }

}