/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.core.alarm.provider;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.skywalking.oap.server.core.alarm.provider.grpc.GRPCAlarmSetting;
import org.apache.skywalking.oap.server.core.alarm.provider.slack.SlackSettings;
import org.yaml.snakeyaml.Yaml;

/**
 * Rule Reader parses the given `alarm-settings.yml` config file, to the target {@link Rules}.
 */
public class RulesReader {
    private Map yamlData;

    public RulesReader(InputStream inputStream) {
        Yaml yaml = new Yaml();
        yamlData = yaml.loadAs(inputStream, Map.class);
    }

    public RulesReader(Reader io) {
        Yaml yaml = new Yaml();
        yamlData = yaml.loadAs(io, Map.class);
    }

    public Rules readRules() {
        Rules rules = new Rules();

        if (Objects.nonNull(yamlData)) {
            Map rulesData = (Map) yamlData.get("rules");
            if (rulesData != null) {
                rules.setRules(new ArrayList<>());
                rulesData.forEach((k, v) -> {
                    if (((String) k).endsWith("_rule")) {
                        AlarmRule alarmRule = new AlarmRule();
                        alarmRule.setAlarmRuleName((String) k);
                        Map settings = (Map) v;
                        Object metricsName = settings.get("metrics-name");
                        if (metricsName == null) {
                            throw new IllegalArgumentException("metrics-name can't be null");
                        }

                        alarmRule.setMetricsName((String) metricsName);
                        alarmRule.setIncludeNames((ArrayList) settings.getOrDefault("include-names", new ArrayList(0)));
                        alarmRule.setExcludeNames((ArrayList) settings.getOrDefault("exclude-names", new ArrayList(0)));
                        alarmRule.setIncludeNamesRegex((String) settings.getOrDefault("include-names-regex", ""));
                        alarmRule.setExcludeNamesRegex((String) settings.getOrDefault("exclude-names-regex", ""));
                        alarmRule.setIncludeLabels(
                            (ArrayList) settings.getOrDefault("include-labels", new ArrayList(0)));
                        alarmRule.setExcludeLabels(
                            (ArrayList) settings.getOrDefault("exclude-labels", new ArrayList(0)));
                        alarmRule.setIncludeLabelsRegex((String) settings.getOrDefault("include-labels-regex", ""));
                        alarmRule.setExcludeLabelsRegex((String) settings.getOrDefault("exclude-labels-regex", ""));
                        alarmRule.setThreshold(settings.get("threshold").toString());
                        alarmRule.setOp((String) settings.get("op"));
                        alarmRule.setPeriod((Integer) settings.getOrDefault("period", 1));
                        alarmRule.setCount((Integer) settings.getOrDefault("count", 1));
                        // How many times of checks, the alarm keeps silence after alarm triggered, default as same as period.
                        alarmRule.setSilencePeriod((Integer) settings.getOrDefault("silence-period", alarmRule.getPeriod()));
                        alarmRule.setMessage(
                            (String) settings.getOrDefault("message", "Alarm caused by Rule " + alarmRule
                                .getAlarmRuleName()));

                        rules.getRules().add(alarmRule);
                    }
                });
            }
            List webhooks = (List) yamlData.get("webhooks");
            if (webhooks != null) {
                rules.setWebhooks(new ArrayList<>());
                webhooks.forEach(url -> {
                    rules.getWebhooks().add((String) url);
                });
            }

            Map grpchooks = (Map) yamlData.get("gRPCHook");
            if (grpchooks != null) {
                GRPCAlarmSetting grpcAlarmSetting = new GRPCAlarmSetting();
                Object targetHost = grpchooks.get("target_host");
                if (targetHost != null) {
                    grpcAlarmSetting.setTargetHost((String) targetHost);
                }

                Object targetPort = grpchooks.get("target_port");
                if (targetPort != null) {
                    grpcAlarmSetting.setTargetPort((Integer) targetPort);
                }

                rules.setGrpchookSetting(grpcAlarmSetting);
            }

            Map slacks = (Map) yamlData.get("slackHooks");
            if (slacks != null) {
                SlackSettings slackSettings = new SlackSettings();
                Object textTemplate = slacks.getOrDefault("textTemplate", "");
                slackSettings.setTextTemplate((String) textTemplate);

                List<String> slackWebhooks = (List<String>) slacks.get("webhooks");
                if (slackWebhooks != null) {
                    slackWebhooks.forEach(
                        url -> slackSettings.getWebhooks().add(url)
                    );
                }

                rules.setSlacks(slackSettings);
            }
        }

        return rules;
    }
}
