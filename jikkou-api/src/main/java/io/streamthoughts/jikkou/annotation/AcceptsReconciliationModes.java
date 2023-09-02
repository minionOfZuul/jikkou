/*
 * Copyright 2023 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.jikkou.annotation;

import static java.lang.annotation.ElementType.TYPE;

import io.streamthoughts.jikkou.api.ReconciliationMode;
import io.streamthoughts.jikkou.api.control.ResourceController;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the reconciliation modes that can be accepted by an
 * {@link ResourceController}.
 *
 * An empty set implies that the controller can accept any reconciliation mode.
 */
@Documented
@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AcceptsReconciliationModes {

    ReconciliationMode[] value();
}