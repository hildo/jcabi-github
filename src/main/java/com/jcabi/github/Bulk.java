/**
 * Copyright (c) 2012-2013, JCabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.github;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import javax.json.JsonObject;
import lombok.EqualsAndHashCode;

/**
 * Bulk items, with pre-saved JSON.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.4
 * @param <T> Type of iterable objects
 * @see <a href="http://developer.github.com/v3/#pagination">Pagination</a>
 */
@EqualsAndHashCode(of = "origin")
final class Bulk<T extends JsonReadable> implements Iterable<T> {

    /**
     * Original iterable.
     */
    private final transient Iterable<T> origin;

    /**
     * Public ctor.
     * @param items Items original
     * @checkstyle AnonInnerLength (50 lines)
     */
    @SuppressWarnings("unchecked")
    Bulk(final Iterable<T> items) {
        if (items instanceof GhPagination) {
            final GhPagination<T> page = GhPagination.class.cast(items);
            final GhPagination.Mapping<T> mapping = page.mapping();
            this.origin = new GhPagination<T>(
                page.request(),
                new GhPagination.Mapping<T>() {
                    @Override
                    public T map(final JsonObject object) {
                        final T item = mapping.map(object);
                        return (T) Proxy.newProxyInstance(
                            Thread.currentThread().getContextClassLoader(),
                            item.getClass().getInterfaces(),
                            new InvocationHandler() {
                                @Override
                                public Object invoke(final Object proxy,
                                    final Method method, final Object[] args) {
                                    final Object result;
                                    if ("json".equals(method.getName())) {
                                        result = object;
                                    } else {
                                        try {
                                            result = method.invoke(item, args);
                                        } catch (IllegalAccessException ex) {
                                            throw new IllegalStateException(ex);
                                        } catch (InvocationTargetException ex) {
                                            throw new IllegalStateException(ex);
                                        }
                                    }
                                    return result;
                                }
                            }
                        );
                    }
                }
            );
        } else {
            this.origin = items;
        }
    }

    @Override
    public String toString() {
        return this.origin.toString();
    }

    @Override
    public Iterator<T> iterator() {
        return this.origin.iterator();
    }

}
