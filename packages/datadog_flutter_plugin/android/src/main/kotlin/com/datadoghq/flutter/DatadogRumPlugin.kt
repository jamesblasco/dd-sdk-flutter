/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.datadoghq.flutter

import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.RumActionType
import com.datadog.android.rum.RumErrorSource
import com.datadog.android.rum.RumMonitor
import com.datadog.android.rum.RumResourceKind
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.lang.ClassCastException

class DatadogRumPlugin(
    rumInstance: RumMonitor? = null
) : MethodChannel.MethodCallHandler {
    companion object RumParameterNames {
        const val PARAM_AT = "at"
        const val PARAM_DURATION = "duration"
        const val PARAM_KEY = "key"
        const val PARAM_VALUE = "value"
        const val PARAM_NAME = "name"
        const val PARAM_ATTRIBUTES = "attributes"
        const val PARAM_URL = "url"
        const val PARAM_HTTP_METHOD = "httpMethod"
        const val PARAM_KIND = "kind"
        const val PARAM_STATUS_CODE = "statusCode"
        const val PARAM_SIZE = "size"
        const val PARAM_MESSAGE = "message"
        const val PARAM_SOURCE = "source"
        const val PARAM_STACK_TRACE = "stackTrace"
        const val PARAM_TYPE = "type"
    }

    private lateinit var channel: MethodChannel
    private lateinit var binding: FlutterPlugin.FlutterPluginBinding

    var rum: RumMonitor? = rumInstance
        private set

    fun setup(
        flutterPluginBinding: FlutterPlugin.FlutterPluginBinding,
        configuration: DatadogFlutterConfiguration.RumConfiguration
    ) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "datadog_sdk_flutter.rum")
        channel.setMethodCallHandler(this)

        binding = flutterPluginBinding

        rum = RumMonitor.Builder()
            .sampleRumSessions(configuration.sampleRate)
            .build()
        GlobalRum.registerIfAbsent(rum!!)
    }

    @Suppress("LongMethod", "ComplexMethod", "ComplexCondition")
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                "startView" -> {
                    val key = call.argument<String>(PARAM_KEY)
                    val name = call.argument<String>(PARAM_NAME)
                    val attributes = call.argument<Map<String, Any?>>(PARAM_ATTRIBUTES)
                    if (key != null && name != null && attributes != null) {
                        rum?.startView(key, name, attributes)
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "stopView" -> {
                    val key = call.argument<String>(PARAM_KEY)
                    val attributes = call.argument<Map<String, Any?>>(PARAM_ATTRIBUTES)
                    if (key != null && attributes != null) {
                        rum?.stopView(key, attributes)
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "addTiming" -> {
                    val name = call.argument<String>(PARAM_NAME)
                    if (name != null) {
                        rum?.addTiming(name)
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "startResourceLoading" -> {
                    val key = call.argument<String>(PARAM_KEY)
                    val url = call.argument<String>(PARAM_URL)
                    val method = call.argument<String>(PARAM_HTTP_METHOD)
                    val attributes = call.argument<Map<String, Any?>>(PARAM_ATTRIBUTES)

                    @Suppress("ComplexCondition")
                    if (key != null && url != null && method != null && attributes != null) {
                        val httpMethod = parseRumHttpMethod(method)
                        rum?.startResource(key, httpMethod, url, attributes)
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "stopResourceLoading" -> {
                    val key = call.argument<String>(PARAM_KEY)
                    val kindString = call.argument<String>(PARAM_KIND)
                    val attributes = call.argument<Map<String, Any?>>(PARAM_ATTRIBUTES)
                    var statusCode = call.argument<Number>(PARAM_STATUS_CODE)
                    var size = call.argument<Number>(PARAM_SIZE)
                    if (key != null && kindString != null && attributes != null) {
                        val kind = parseRumResourceKind(kindString)
                        rum?.stopResource(
                            key,
                            statusCode?.toInt(),
                            size?.toLong(),
                            kind,
                            attributes
                        )
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "stopResourceLoadingWithError" -> {
                    val key = call.argument<String>(PARAM_KEY)
                    val message = call.argument<String>(PARAM_MESSAGE)
                    val errorType = call.argument<String>(PARAM_TYPE)
                    val attributes = call.argument<Map<String, Any?>>(PARAM_ATTRIBUTES)
                    if (key != null && message != null && errorType != null && attributes != null) {
                        rum?.stopResourceWithError(
                            key, null, message, RumErrorSource.NETWORK,
                            "", errorType, attributes
                        )
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "addError" -> {
                    val message = call.argument<String>(PARAM_MESSAGE)
                    val sourceString = call.argument<String>(PARAM_SOURCE)
                    val attributes = call.argument<Map<String, Any?>>(PARAM_ATTRIBUTES)
                    var stackTrace = call.argument<String>(PARAM_STACK_TRACE)
                    if (message != null && sourceString != null && attributes != null) {
                        val source = parseRumErrorSource(sourceString)
                        rum?.addErrorWithStacktrace(message, source, stackTrace, attributes)
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "addUserAction" -> {
                    val typeString = call.argument<String>(PARAM_TYPE)
                    val name = call.argument<String>(PARAM_NAME)
                    val attributes = call.argument<Map<String, Any?>>(PARAM_ATTRIBUTES)
                    if (typeString != null && name != null && attributes != null) {
                        val actionType = parseRumActionType(typeString)
                        rum?.addUserAction(actionType, name, attributes)
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "startUserAction" -> {
                    val typeString = call.argument<String>(PARAM_TYPE)
                    val name = call.argument<String>(PARAM_NAME)
                    val attributes = call.argument<Map<String, Any?>>(PARAM_ATTRIBUTES)
                    if (typeString != null && name != null && attributes != null) {
                        val actionType = parseRumActionType(typeString)
                        rum?.startUserAction(actionType, name, attributes)
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "stopUserAction" -> {
                    val typeString = call.argument<String>(PARAM_TYPE)
                    val name = call.argument<String>(PARAM_NAME)
                    val attributes = call.argument<Map<String, Any?>>(PARAM_ATTRIBUTES)
                    if (typeString != null && name != null && attributes != null) {
                        val actionType = parseRumActionType(typeString)
                        rum?.stopUserAction(actionType, name, attributes)
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "addAttribute" -> {
                    val key = call.argument<String>(PARAM_KEY)
                    val value = call.argument<Any>(PARAM_VALUE)
                    if (key != null && value != null) {
                        GlobalRum.addAttribute(key, value)
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "removeAttribute" -> {
                    val key = call.argument<String>(PARAM_KEY)
                    if (key != null) {
                        GlobalRum.removeAttribute(key)
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                "reportLongTask" -> {
                    val at = call.argument<Long>(PARAM_AT)
                    val duration = call.argument<Int>(PARAM_DURATION)
                    if (at != null && duration != null) {
                        // Duration is in ms, convert to ns
                        val durationNs = duration.toLong() * 1000
                        rum?._getInternal()?.addLongTask(durationNs, "")
                        result.success(null)
                    } else {
                        result.missingParameter(call.method)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        } catch (e: ClassCastException) {
            result.error(
                DatadogSdkPlugin.CONTRACT_VIOLATION, e.toString(),
                mapOf(
                    "methodName" to call.method
                )
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun teardown(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}

fun parseRumHttpMethod(value: String): String {
    return when (value) {
        "RumHttpMethod.get" -> "GET"
        "RumHttpMethod.post" -> "POST"
        "RumHttpMethod.head" -> "HEAD"
        "RumHttpMethod.put" -> "PUT"
        "RumHttpMethod.delete" -> "DELETE"
        "RumHttpMethod.patch" -> "PATCH"
        else -> "GET"
    }
}

fun parseRumResourceKind(value: String): RumResourceKind {
    return when (value) {
        "RumResourceType.document" -> RumResourceKind.DOCUMENT
        "RumResourceType.image" -> RumResourceKind.IMAGE
        "RumResourceType.xhr" -> RumResourceKind.XHR
        "RumResourceType.beacon" -> RumResourceKind.BEACON
        "RumResourceType.css" -> RumResourceKind.CSS
        "RumResourceType.fetch" -> RumResourceKind.FETCH
        "RumResourceType.font" -> RumResourceKind.FONT
        "RumResourceType.js" -> RumResourceKind.JS
        "RumResourceType.media" -> RumResourceKind.MEDIA
        "RumResourceType.native" -> RumResourceKind.NATIVE
        else -> RumResourceKind.OTHER
    }
}

fun parseRumErrorSource(value: String): RumErrorSource {
    return when (value) {
        "RumErrorSource.source" -> RumErrorSource.SOURCE
        "RumErrorSource.network" -> RumErrorSource.NETWORK
        "RumErrorSource.webview" -> RumErrorSource.WEBVIEW
        "RumErrorSource.console" -> RumErrorSource.CONSOLE
        "RumErrorSource.custom" -> RumErrorSource.SOURCE
        else -> RumErrorSource.SOURCE
    }
}

fun parseRumActionType(value: String): RumActionType {
    return when (value) {
        "RumUserActionType.tap" -> RumActionType.TAP
        "RumUserActionType.scroll" -> RumActionType.SCROLL
        "RumUserActionType.swipe" -> RumActionType.SWIPE
        "RumUserActionType.custom" -> RumActionType.CUSTOM
        else -> RumActionType.CUSTOM
    }
}
