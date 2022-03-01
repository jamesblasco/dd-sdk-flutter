/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.datadoghq.flutter

import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import kotlin.reflect.typeOf

@ExtendWith(ForgeExtension::class)
@OptIn(kotlin.ExperimentalStdlibApi::class)
class DatadogLogsPluginTest {
    private lateinit var plugin: DatadogLogsPlugin

    @BeforeEach
    fun beforeEach() {
        plugin = DatadogLogsPlugin()
        plugin.setupForTests()
    }

    @Test
    fun `M report a contract violation W onMethodCall(log) parameter is of wrong type`(
        forge: Forge,
        @IntForgery message: Int
    ) {
        // GIVEN
        val method = forge.anElementFrom( listOf("debug", "info", "warn", "error") )
        val call = MethodCall(method, mapOf(
            "message" to Int,
            "context" to mapOf<String, Any>()
        ))
        val mockResult = mock<MethodChannel.Result>()

        // WHEN
        plugin.onMethodCall(call, mockResult)

        // THEN
        verify(mockResult).error(eq(DatadogSdkPlugin.CONTRACT_VIOLATION), any(), anyOrNull())
    }

    private val contracts = listOf(
        Contract("debug", mapOf(
            "message" to typeOf<String>(), "context" to typeOf<Map<String, Any?>>()
        )),
        Contract("info", mapOf(
            "message" to typeOf<String>(), "context" to typeOf<Map<String, Any?>>()
        )),
        Contract("warn", mapOf(
            "message" to typeOf<String>(), "context" to typeOf<Map<String, Any?>>()
        )),
        Contract("error", mapOf(
            "message" to typeOf<String>(), "context" to typeOf<Map<String, Any?>>()
        )),
        Contract("addAttribute", mapOf(
            "key" to typeOf<String>(), "value" to typeOf<Map<String, Any?>>()
        )),
        Contract("addTag", mapOf(
            "tag" to typeOf<String>()
        )),
        Contract("removeAttribute", mapOf(
            "key" to typeOf<String>()
        )),
        Contract("removeTag", mapOf(
            "tag" to typeOf<String>()
        )),
        Contract("removeTagWithKey", mapOf(
            "key" to typeOf<String>()
        )),
    )

    @Test
    fun `M report contract violation W missing parameters in contract`(
        forge: Forge
    ) {
        testContracts(contracts, forge, plugin)
    }
}