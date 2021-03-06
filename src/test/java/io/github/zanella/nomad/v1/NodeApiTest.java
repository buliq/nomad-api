package io.github.zanella.nomad.v1;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.zanella.nomad.v1.common.models.Constraint;
import io.github.zanella.nomad.v1.common.models.Job;
import io.github.zanella.nomad.v1.common.models.TaskState;
import io.github.zanella.nomad.v1.common.models.UpdateStrategy;
import io.github.zanella.nomad.v1.nodes.NodeApi;
import io.github.zanella.nomad.v1.nodes.models.NodeAllocation;
import io.github.zanella.nomad.v1.nodes.models.NodeDrainEvalResult;
import io.github.zanella.nomad.v1.nodes.models.NodeEvalResult;
import io.github.zanella.nomad.v1.nodes.models.NodeInfo;
import io.github.zanella.nomad.v1.nodes.models.Resources;
import io.github.zanella.nomad.v1.nodes.models.Service;
import io.github.zanella.nomad.v1.nodes.models.Task;
import io.github.zanella.nomad.v1.nodes.models.TaskGroup;

import org.junit.Test;

import java.util.List;
import java.util.Map;

public class NodeApiTest extends AbstractCommon {

    private static Map<String, String> makeAttributes() {
        return ImmutableMap.<String, String>builder()
                .put("arch", "amd64")
                .put("cpu.frequency", "1300.000000")
                .put("cpu.modelname", "Intel(R) Core(TM) i5-4250U CPU @ 1.30GHz")
                .put("cpu.numcores", "2")
                .put("cpu.totalcompute", "2600.000000")
                .put("driver.exec", "1")
                .put("driver.java", "1")
                .put("driver.java.runtime", "Java(TM) SE Runtime Environment (build 1.8.0_05-b13)")
                .put("driver.java.version", "1.8.0_05")
                .put("driver.java.vm", "Java HotSpot(TM) 64-Bit Server VM (build 25.5-b02, mixed mode)")
                .put("hostname", "Armons-MacBook-Air.local")
                .put("kernel.name", "darwin")
                .put("kernel.version", "14.4.0")
                .put("memory.totalbytes", "8589934592")
                .put("network.ip-address", "127.0.0.1")
                .put("os.name", "darwin")
                .put("os.version", "14.4.0")
                .put("storage.bytesfree", "35888713728")
                .put("storage.bytestotal", "249821659136")
                .put("storage.volume", "/dev/disk1")
                .build();
    }

    public static NodeInfo newNodeInfo() {
        final NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setId("c9972143-861d-46e6-df73-1d8287bc3e66");
        nodeInfo.setDatacenter("dc1");
        nodeInfo.setName("Armons-MacBook-Air.local");
        nodeInfo.setHttpAddr("127.0.0.1:4646");
        nodeInfo.setTlsEnabled(false);

        nodeInfo.setAttributes( makeAttributes() );
        nodeInfo.setResources( new Resources(2600, 8192, 34226, 0, null) );
        nodeInfo.setReserved(new Resources(2600, 8192, 34226, 0, null));
        nodeInfo.setLinks( ImmutableMap.of() );
        nodeInfo.setMeta( ImmutableMap.builder().build() );

        nodeInfo.setNodeClass("");
        nodeInfo.setDrain(false);
        nodeInfo.setStatus("ready");
        nodeInfo.setStatusDescription("");
        nodeInfo.setCreateIndex(3);
        nodeInfo.setModifyIndex(4);

        return nodeInfo;
    }

    private final NodeInfo expectedNodeInfo = newNodeInfo();

    @Test
    public void getNodeTest() {
        final String rawNodeSummary = "{" +
                "\"ID\": \"c9972143-861d-46e6-df73-1d8287bc3e66\", \"Datacenter\": \"dc1\"," +
                "\"Name\": \"Armons-MacBook-Air.local\"," +
                "\"HTTPAddr\": \"127.0.0.1:4646\"," +
                "\"TLSEnabled\": false, " +
                "\"Attributes\": {" +
                "    \"arch\": \"amd64\", \"cpu.frequency\": \"1300.000000\"," +
                "    \"cpu.modelname\": \"Intel(R) Core(TM) i5-4250U CPU @ 1.30GHz\",\"cpu.numcores\": \"2\"," +
                "    \"cpu.totalcompute\": \"2600.000000\", \"driver.exec\": \"1\", \"driver.java\": \"1\"," +
                "    \"driver.java.runtime\": \"Java(TM) SE Runtime Environment (build 1.8.0_05-b13)\"," +
                "    \"driver.java.version\": \"1.8.0_05\"," +
                "    \"driver.java.vm\": \"Java HotSpot(TM) 64-Bit Server VM (build 25.5-b02, mixed mode)\"," +
                "    \"hostname\": \"Armons-MacBook-Air.local\", \"kernel.name\": \"darwin\", \"kernel.version\": \"14.4.0\"," +
                "    \"memory.totalbytes\": \"8589934592\", \"network.ip-address\": \"127.0.0.1\", \"os.name\": \"darwin\"," +
                "    \"os.version\": \"14.4.0\", \"storage.bytesfree\": \"35888713728\"," +
                "    \"storage.bytestotal\": \"249821659136\", \"storage.volume\": \"/dev/disk1\"" +
                "}," +
                "\"Resources\": {\"CPU\": 2600, \"MemoryMB\": 8192, \"DiskMB\": 34226, \"IOPS\": 0, \"Networks\": null}," +
                "\"Reserved\": {\"CPU\": 2600, \"MemoryMB\": 8192, \"DiskMB\": 34226, \"IOPS\": 0, \"Networks\": null}," +
                "\"Links\": {}, \"Meta\": {}, \"NodeClass\": \"\", \"Drain\": false," +
                "\"Status\": \"ready\", \"StatusDescription\": \"\", \"CreateIndex\": 3, \"ModifyIndex\": 4" +
                "}";

        stubFor(get(urlEqualTo(UriTemplate.fromTemplate(NodeApi.nodeUrl).expand(ImmutableMap.of("nodeId", "nodeId"))))
                        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(rawNodeSummary))
        );

        assertEquals(expectedNodeInfo, nomadClient.v1.node.getNode("nodeId"));
    }

    public static NodeAllocation createNodeAllocation() {
        final Job job = new Job();
        final Resources commonResources;

        {
            job.setId("example");
            job.setName("example");
            job.setType("service");
            job.setPriority(50);
            job.setStatus("");
            job.setStatusDescription("");
            job.setCreateIndex(5);
            job.setModifyIndex(5);

            job.setMeta(null);
            job.setUpdate( new UpdateStrategy(1, 1e+10) );

            commonResources = new Resources(500, 256, 0, 0, ImmutableList.of(
                    new Resources.Network(
                            ImmutableList.of(new Resources.Network.DynamicPort(20802, "db")), null, 0, "127.0.0.1", "","lo")));

            final Task task = Task.builder()
                .resources(commonResources)
                .driver("docker")
                .name("redis")
                .config(Task.Config.builder()
                    .portMap(ImmutableList.of(((Map<String, Integer>) ImmutableMap.of("db", 6379))))
                    .image("redis:latest")
                .build())
                .services(ImmutableList.of(new Service(
                        ImmutableList.of(new Service.Check((long) 2e+09, (long) 1e+10, "", "", "", "tcp", "alive", "")),
                        "db", ImmutableList.of("global", "cache"), "example-cache-redis", "")))
            .build();

            job.setTaskGroups( ImmutableList.of(new TaskGroup(
                    null, ImmutableList.of(task), new TaskGroup.RestartPolicy(25000000000L, 300000000000L, 10, "fail"), null, null, 1, "cache")));

            job.setRegion("global");
            job.setAllAtOnce(false);
            job.setDatacenters( ImmutableList.of("dc1") );
            job.setConstraints( ImmutableList.of(new Constraint("=", "linux", "$attr.kernel.name") ));
        }

        final NodeAllocation expectedNodeAllocation = new NodeAllocation();

        expectedNodeAllocation.setId("203266e5-e0d6-9486-5e05-397ed2b184af");
        expectedNodeAllocation.setEvalId("e68125ed-3fba-fb46-46cc-291addbc4455");
        expectedNodeAllocation.setName("example.cache[0]");
        expectedNodeAllocation.setNodeId("e02b6169-83bd-9df6-69bd-832765f333eb");
        expectedNodeAllocation.setJobId("example");
        expectedNodeAllocation.setModifyIndex(9);

        expectedNodeAllocation.setResources(new Resources(500, 256, 0, 0, ImmutableList.of(
                new Resources.Network(
                        ImmutableList.of(new Resources.Network.DynamicPort(20802, "db")), null, 10, "", "",""))));

        expectedNodeAllocation.setTaskGroup("cache");

        expectedNodeAllocation.setJob(job);
        expectedNodeAllocation.setTaskResources(ImmutableMap.of("redis", commonResources));

        expectedNodeAllocation.setMetrics(
                new NodeAllocation.Metrics(0, 1590406, 1, 0,  null, null, 0, null, null,
                        ImmutableMap.of("e02b6169-83bd-9df6-69bd-832765f333eb.binpack", 6.133651487695705)));

        expectedNodeAllocation.setDesiredStatus("run");
        expectedNodeAllocation.setDesiredDescription("");
        expectedNodeAllocation.setClientStatus("running");
        expectedNodeAllocation.setClientDescription("");

        expectedNodeAllocation.setTaskStates(
                ImmutableMap.of("redis",
                        new TaskState(
                                ImmutableList.of(new TaskState.Event("", "", 0L, "", 0, 0, "", 1447806038427841000L, "Started", false, "", "", "", "")),
                                "running", false)));
        expectedNodeAllocation.setCreateIndex(7);

        return expectedNodeAllocation;
    }

    @Test
    public void getNodeAllocationsTest() {
        final String rawNodeAllocation = "[ {" +
                "  \"ID\": \"203266e5-e0d6-9486-5e05-397ed2b184af\", \"EvalID\": \"e68125ed-3fba-fb46-46cc-291addbc4455\"," +
                "  \"Name\": \"example.cache[0]\", \"NodeID\": \"e02b6169-83bd-9df6-69bd-832765f333eb\"," +
                "  \"JobID\": \"example\", \"ModifyIndex\": 9," +
                "  \"Resources\": {" +
                "    \"Networks\": [ {" +
                "        \"DynamicPorts\": [ {\"Value\": 20802, \"Label\": \"db\"} ]," +
                "        \"ReservedPorts\": null, \"MBits\": 10, \"IP\": \"\", \"CIDR\": \"\", \"Device\": \"\"" +
                "      } ]," +
                "    \"IOPS\": 0, \"DiskMB\": 0, \"MemoryMB\": 256, \"CPU\": 500" +
                "  }," +
                "  \"TaskGroup\": \"cache\"," +
                "  \"Job\": {" +
                "    \"ModifyIndex\": 5, \"CreateIndex\": 5, \"StatusDescription\": \"\"," +
                "    \"Status\": \"\", \"Meta\": null," +
                "    \"Update\": {\"MaxParallel\": 1, \"Stagger\": 1e+10}," +
                "    \"TaskGroups\": [ {" +
                "        \"Meta\": null," +
                "        \"Tasks\": [ {" +
                "            \"Meta\": null," +
                "            \"Resources\": {" +
                "              \"Networks\": [ {" +
                "                  \"DynamicPorts\": [ {\"Value\": 20802, \"Label\": \"db\"} ]," +
                "                  \"ReservedPorts\": null, \"MBits\": 0, \"IP\": \"127.0.0.1\"," +
                "                  \"CIDR\": \"\", \"Device\": \"lo\"" +
                "                } ]," +
                "              \"IOPS\": 0, \"DiskMB\": 0, \"MemoryMB\": 256, \"CPU\": 500" +
                "            }," +
                "            \"Constraints\": null," +
                "            \"Services\": [ {" +
                "                \"Checks\": [ {" +
                "                    \"Timeout\": 2e+09, \"Interval\": 1e+10, \"Protocol\": \"\"," +
                "                    \"Http\": \"\", \"Script\": \"\", \"Type\": \"tcp\"," +
                "                    \"Name\": \"alive\", \"Id\": \"\", \"Path\": \"\"" +
                "                  } ]," +
                "                \"PortLabel\": \"db\",\"Tags\": [ \"global\", \"cache\"]," +
                "                \"Name\": \"example-cache-redis\", \"Id\": \"\"" +
                "              } ]," +
                "            \"Env\": null," +
                "            \"Config\": { \"port_map\": [ {\"db\": 6379} ], \"image\": \"redis:latest\"}," +
                "            \"Driver\": \"docker\", \"Name\": \"redis\"" +
                "          } ]," +
                "        \"RestartPolicy\": {\"Delay\": 2.5e+10, \"Interval\": 3e+11, \"Attempts\": 10, \"Mode\": \"fail\"}," +
                "        \"Constraints\": null,\"Count\": 1,\"Name\": \"cache\"" +
                "      } ]," +
                "    \"Region\": \"global\", \"ID\": \"example\", \"Name\": \"example\", \"Type\": \"service\"," +
                "    \"Priority\": 50, \"AllAtOnce\": false," +
                "    \"Datacenters\": [\"dc1\"]," +
                "    \"Constraints\": [ {\"Operand\": \"=\", \"RTarget\": \"linux\", \"LTarget\": \"$attr.kernel.name\"} ]" +
                "  }," +
                "  \"TaskResources\": {" +
                "    \"redis\": {" +
                "      \"Networks\": [ {" +
                "          \"DynamicPorts\": [ {\"Value\": 20802, \"Label\": \"db\"} ]," +
                "          \"ReservedPorts\": null, \"MBits\": 0, \"IP\": \"127.0.0.1\", \"CIDR\": \"\", \"Device\": \"lo\"" +
                "        } ]," +
                "      \"IOPS\": 0, \"DiskMB\": 0, \"MemoryMB\": 256, \"CPU\": 500" +
                "    }" +
                "  }," +
                "  \"Metrics\": {" +
                "    \"CoalescedFailures\": 0, \"AllocationTime\": 1590406, \"NodesEvaluated\": 1," +
                "    \"NodesFiltered\": 0, \"ClassFiltered\": null, \"ConstraintFiltered\": null," +
                "    \"NodesExhausted\": 0, \"ClassExhausted\": null, \"DimensionExhausted\": null," +
                "    \"Scores\": {\"e02b6169-83bd-9df6-69bd-832765f333eb.binpack\": 6.133651487695705}" +
                "  }," +
                "  \"DesiredStatus\": \"run\", \"DesiredDescription\": \"\", \"ClientStatus\": \"running\"," +
                "  \"ClientDescription\": \"\"," +
                "  \"TaskStates\": {" +
                "    \"redis\": {" +
                "      \"Events\": [\n" +
                "        {\n" +
                "          \"KillError\": \"\",\n" +
                "          \"KillReason\": \"\",\n" +
                "          \"KillTimeout\": 0,\n" +
                "          \"Message\": \"\",\n" +
                "          \"Signal\": 0,\n" +
                "          \"ExitCode\": 0,\n" +
                "          \"DriverError\": \"\",\n" +
                "          \"Time\": 1447806038427841000,\n" +
                "          \"Type\": \"Started\",\n" +
                "          \"FailsTask\": false,\n" +
                "          \"TaskSignalReason\": \"\",\n" +
                "          \"TaskSignal\": \"\",\n" +
                "          \"ValidationError\": \"\",\n" +
                "          \"DownloadError\": \"\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"State\": \"running\"," +
                "      \"Failed\": false" +
                "    }" +
                "  }," +
                "  \"CreateIndex\": 7" +
                "} ]";

        stubFor(get(urlEqualTo(UriTemplate.fromTemplate(NodeApi.allocationsUrl).expand(ImmutableMap.of("nodeId", "nodeId"))))
                        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(rawNodeAllocation))
        );

        final NodeAllocation expectedNodeAllocation = createNodeAllocation();

        final List<NodeAllocation> actualNodeAllocations = nomadClient.v1.node.getNodeAllocations("nodeId");

        final List<NodeAllocation> expectedNodeAllocationList = ImmutableList.of(expectedNodeAllocation);

        final String originalId = expectedNodeAllocation.getId();

        final Integer originalCreateIndex = expectedNodeAllocation.getJob().getCreateIndex();

        //
        expectedNodeAllocation.setId("blah");
        expectedNodeAllocation.getJob().setCreateIndex(4);
        assertNotEquals(expectedNodeAllocationList, actualNodeAllocations);

        //
        expectedNodeAllocation.getJob().setCreateIndex(originalCreateIndex);
        assertNotEquals(expectedNodeAllocationList, actualNodeAllocations);

        //
        expectedNodeAllocation.setId(originalId);
        assertEquals(expectedNodeAllocationList, actualNodeAllocations);
    }

    @Test
    public void putEvaluateTest() {
        final  String rawEvaluate = "{" +
                "\"EvalIDs\": [\"d092fdc0-e1fd-2536-67d8-43af8ca798ac\"]," +
                "\"EvalCreateIndex\": 35, \"NodeModifyIndex\": 34" +
                "}";

        stubFor(put(urlEqualTo(UriTemplate.fromTemplate(NodeApi.evaluateUrl).expand(ImmutableMap.of("nodeId", "nodeId"))))
                        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(rawEvaluate))
        );

        assertEquals(new NodeEvalResult(ImmutableList.of("d092fdc0-e1fd-2536-67d8-43af8ca798ac"), 35, 34),
                nomadClient.v1.node.putEvaluate("nodeId"));
    }

    @Test
    public void putDrainTest() {
        final String rawEvaluate = "{" +
                "\"EvalID\": \"d092fdc0-e1fd-2536-67d8-43af8ca798ac\"," +
                "\"EvalCreateIndex\": 35, \"NodeModifyIndex\": 34" +
                "}";

        stubFor(put(urlEqualTo(UriTemplate.fromTemplate(NodeApi.drainUrl).expand(ImmutableMap.of("nodeId", "nodeId", "enableSwitch", true))))
                        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(rawEvaluate))
        );

        assertEquals(new NodeDrainEvalResult("d092fdc0-e1fd-2536-67d8-43af8ca798ac", 35, 34),
                nomadClient.v1.node.putDrain("nodeId", true));
    }
}
