/*
 * Copyright (c) 2009-2014, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalman.equation;

/**
 * Variables which have special meanings depending on other matrices
 *
 * @author Peter Abeles
 */
public class VariableSpecial extends Variable {

    Special type;

    protected VariableSpecial( Special special) {
        super(VariableType.SPECIAL);
        this.type = special;
    }

    public Special getValue() {
        return type;
    }

    public static enum Special {
        /** Maximum possible value */
        END,
        /** All values */
        ALL
    }
}
