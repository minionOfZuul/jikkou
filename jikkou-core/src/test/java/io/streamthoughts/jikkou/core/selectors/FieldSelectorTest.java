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
package io.streamthoughts.jikkou.core.selectors;

import io.streamthoughts.jikkou.core.TestResource;
import io.streamthoughts.jikkou.core.models.ObjectMeta;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FieldSelectorTest {

    static final TestResource TEST_RESOURCE = new TestResource()
            .withMetadata(ObjectMeta
                    .builder()
                    .withName("test-resource")
                    .withLabel("a-label.key", "value")
                    .withAnnotation("an-annotation.key", "value")
                    .build()
            );


    @Test
    public void shouldSelectResourceForSelectorIn() {
        // GIVEN
        var expression =  new SelectorExpression(
                "",
                "",
                "metadata.name",
                ExpressionOperator.IN, (List.of("test-resource"))
        );
        FieldSelector selector = new FieldSelector(expression);

        // WHEN
        boolean result = selector.apply(TEST_RESOURCE);

        // THEN
        Assertions.assertTrue(result);
    }

    @Test
    public void shouldNotSelectResourceForSelectorNotIn() {
        // GIVEN
        var expression =  new SelectorExpression(
                "",
                "",
                "metadata.name",
                ExpressionOperator.NOTIN, (List.of("test-resource"))
        );
        FieldSelector selector = new FieldSelector(expression);

        // WHEN
        boolean result = selector.apply(TEST_RESOURCE);

        // THEN
        Assertions.assertFalse(result);
    }
}