/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A Java Cleaner wrapper class to let it work with Android version less then 33
 *
 * @author Kanstantsin Valeitsenak
 */

package com.ainceborn.pdfbox.util;

import android.os.Build;

import java.lang.ref.Cleaner;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CleanerCompat {

    public static CleanerWrapper create(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new JavaCleaner(Cleaner.create());
        } else return new LegacyCleanerWrapper();
    }

    public static class JavaCleaner extends CleanerWrapper {

        private Cleaner cleaner;
        JavaCleaner(Cleaner cleaner){
            this.cleaner = cleaner;
        }
        @Override
        public Cleaner.Cleanable register(Object obj, Runnable action) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return cleaner.register(obj, action);
            } return null;
        }
    }

    public static class LegacyCleanerWrapper extends CleanerWrapper{
        private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
        private final Map<PhantomReference<?>, Runnable> actions = new ConcurrentHashMap<>();
        private final Thread cleanerThread;

        public LegacyCleanerWrapper(){
            cleanerThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        PhantomReference<?> ref = (PhantomReference<?>) queue.remove();
                        Runnable action = actions.remove(ref);
                        if (action != null) {
                            action.run();
                        }
                        ref.clear();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "LegacyCleanerWrapper");

            cleanerThread.setDaemon(true);
            cleanerThread.start();
        }


        @Override
        public Cleaner.Cleanable register(Object obj, Runnable action) {
            PhantomReference<Object> ref = new PhantomReference<>(obj, queue);
            actions.put(ref, action);
            return () -> {
                actions.remove(ref);
                ref.clear();
            };
        }
    }

    public static abstract class CleanerWrapper {
        public abstract Cleaner.Cleanable register(Object obj, Runnable action);
    }
}
