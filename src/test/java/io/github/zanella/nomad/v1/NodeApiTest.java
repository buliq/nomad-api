package io.github.zanella.nomad.v1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.zanella.nomad.v1.common.models.EvalResult;
import io.github.zanella.nomad.v1.nodes.NodeApi;
import io.github.zanella.nomad.v1.nodes.models.*;
import org.junit.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

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

        nodeInfo.setAttributes( makeAttributes() );
        nodeInfo.setResources( new Resources(2600, 8192, 34226, 0, null) );
        nodeInfo.setReserved(null);
        nodeInfo.setLinks( ImmutableMap.builder().build() );
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

    private final static String rawEvaluate = "{" +
            "\"EvalIDs\": [\"d092fdc0-e1fd-2536-67d8-43af8ca798ac\"]," +
            "\"EvalCreateIndex\": 35," +
            "\"NodeModifyIndex\": 34" +
            "}";

    private final EvalResult expectedEvalResult =
            new EvalResult(ImmutableList.of("d092fdc0-e1fd-2536-67d8-43af8ca798ac"), 35, 34);

    @Test
    public void getNodeTest() {
        final String rawNodeSummary = "{" +
                "\"ID\": \"c9972143-861d-46e6-df73-1d8287bc3e66\", \"Datacenter\": \"dc1\"," +
                "\"Name\": \"Armons-MacBook-Air.local\"," +
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
                "\"Reserved\": null, \"Links\": {}, \"Meta\": {}, \"NodeClass\": \"\", \"Drain\": false," +
                "\"Status\": \"ready\", \"StatusDescription\": \"\", \"CreateIndex\": 3, \"ModifyIndex\": 4" +
                "}";

        stubFor(get(urlEqualTo(NodeApi.nodeUrl + "/42"))
                        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(rawNodeSummary))
        );

        assertEquals(expectedNodeInfo, nomadClient.v1.node.getNode("42"));
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
                "                    \"Name\": \"alive\", \"Id\": \"\"" +
                "                  } ]," +
                "                \"PortLabel\": \"db\",\"Tags\": [ \"global\", \"cache\"]," +
                "                \"Name\": \"example-cache-redis\", \"Id\": \"\"" +
                "              } ]," +
                "            \"Env\": null," +
                "            \"Config\": { \"port_map\": [ {\"db\": 6379} ], \"image\": \"redis:latest\"}," +
                "            \"Driver\": \"docker\", \"Name\": \"redis\"" +
                "          } ]," +
                "        \"RestartPolicy\": {\"Delay\": 2.5e+10, \"Interval\": 3e+11, \"Attempts\": 10}," +
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
                "      \"Events\": [ {" +
                "          \"KillError\": \"\", \"Message\": \"\", \"Signal\": 0, \"ExitCode\": 0," +
                "          \"DriverError\": \"\", \"Time\": 1447806038427841000, \"Type\": \"Started\"" +
                "        } ]," +
                "      \"State\": \"running\"" +
                "    }" +
                "  }," +
                "  \"CreateIndex\": 7" +
                "} ]";

        stubFor(get(urlEqualTo(NodeApi.nodeUrl + "/42" + NodeApi.allocationsUrl))
                        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(rawNodeAllocation))
        );

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

        final NodeJob nodeJob = new NodeJob();
        nodeJob.setId("example");
        nodeJob.setName("example");
        nodeJob.setType("service");
        nodeJob.setPriority(50);
        nodeJob.setStatus("");
        nodeJob.setStatusDescription("");
        nodeJob.setCreateIndex(5);
        nodeJob.setModifyIndex(5);

        nodeJob.setMeta(null);
        nodeJob.setUpdate( new NodeJob.Update(1, 1e+10) );

        final Resources commonResources = new Resources(500, 256, 0, 0, ImmutableList.of(
                new Resources.Network(
                        ImmutableList.of(new Resources.Network.DynamicPort(20802, "db")), null, 0, "127.0.0.1", "","lo")));
        //
        final Task task = new Task(
                null,
                commonResources,
                null,
                ImmutableList.of(new Service(
                        ImmutableList.of(new Service.Check(2e+09, 1e+10, "", "", "", "tcp", "alive", "")),
                        "db", ImmutableList.of("global", "cache"), "example-cache-redis", "")),
                null,
                new Task.Config(ImmutableList.of(((Map<String, Integer>) ImmutableMap.of("db", 6379))), "redis:latest"),
                "docker",
                "redis");

        nodeJob.setTaskGroup( ImmutableList.of(new TaskGroup(
                        null, ImmutableList.of(task), new TaskGroup.RestartPolicy(2.5e+10, 3e+11, 10), null, 1, "cache")));

        nodeJob.setRegion("global");
        nodeJob.setAllAtOnce(false);
        nodeJob.setDatacenters( ImmutableList.of("dc1") );
        nodeJob.setConstraints( ImmutableList.of(new NodeJob.Constraint("=", "linux", "$attr.kernel.name") ));

        expectedNodeAllocation.setNodeJob(nodeJob);
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
                                ImmutableList.of(new TaskState.Event("", "", 0, 0, "", 1447806038427841000L, "Started")),
                                "running")));
        expectedNodeAllocation.setCreateIndex(7);

        //
        assertEquals(expectedNodeAllocation, nomadClient.v1.node.getNodeAllocations("42").get(0));
    }

    @Test
    public void putEvaluateTest() {
        stubFor(put(urlEqualTo(NodeApi.nodeUrl + "/42" + NodeApi.evaluateUrl))
                        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(rawEvaluate))
        );

        assertEquals(expectedEvalResult, nomadClient.v1.node.putEvaluate("42"));
    }

    @Test
    public void putDrainTest() {
        stubFor(put(urlEqualTo(NodeApi.nodeUrl + "/42" + NodeApi.drainUrl + "?enable=true"))
                        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(rawEvaluate))
        );

        assertEquals(expectedEvalResult, nomadClient.v1.node.putDrain("42", true));
    }
}