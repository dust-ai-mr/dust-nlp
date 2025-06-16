/*
 *
 *  Copyright 2024-Present Alan Littleford
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package com.mentalresonance.dust.nlp.genericgpt;

import com.mentalresonance.dust.core.actors.ActorRef;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
public class GPTMsg implements Serializable {
    /**
     * The request to the endpoint
     */
    protected String request;

    /**
     * The model to use
     */
    @Getter
    @Setter
    protected String model;

    /**
     * Error message if any
     */
    @Setter
    protected String error = null;

    /**
     * Optional API key. If not null overrides key in service actor
     */
    @Getter
    protected String key = null;

    public Serializable setKey(String key)
    {
        this.key = key;
        return this;
    }

    /**
     * Optional - actual key used in request
     */
    @Getter
    @Setter
    protected String bearer;

    /**
     * Optional ref for Accounting. If not null the accounting Actor gets
     */
    @Getter
    protected ActorRef accountingRef = null;

    public Serializable setAccountingRef(ActorRef accountingRef)
    {
        this.accountingRef = accountingRef;
        return this;
    }

}
