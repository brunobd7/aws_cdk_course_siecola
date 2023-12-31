package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

public class Service01Stack extends Stack {
    public Service01Stack(final Construct scope, final String id, Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster) {
        super(scope, id, props);


        ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.Builder
                .create(this, "ALB01")// CREATING A APPLICATION LOAD BALANCE
                .serviceName("service-01") // CREATING A SERVICE
                .cluster(cluster)
                .cpu(512)// vCPU
                .memoryLimitMiB(1024)// vRAM
                .desiredCount(2)//INSTANCES
                .listenerPort(8080)// APPLICATION PORT -> TO EXTERNAL ACCESS
                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder() // CREATING A TASK TO SPECIFIED HOW OUR SERVICE WILL BE EXECUTE
                        .containerName("aws_project01")//APPLICATION CONTAINER NAME
                        .containerPort(8080)//APPLICATION CONTAINER PORT
                        .image(ContainerImage.fromRegistry("siecola/curso_aws_project01:1.7.0"))//DOCKER IMAGE FROM REPO (DOCKER_HUB)
                        .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder
                                        .create(this, "Service01LogGroup")
                                        .logGroupName("Service01")
                                        .removalPolicy(RemovalPolicy.DESTROY)//RULE TO PERSIST OR DESTROY LOGS
                                        .build())
                                        .streamPrefix("Service01")
                                .build()))//CONFIGURING LOGS WITH AWS CLOUDWATCH
                        .build())
                .publicLoadBalancer(true)// SET THIS LOADBALANCER AS PUBLIC
                .build();

        //CONFIGURING A HEALTHCHECK USING SPRING ACTUATOR PATH REFERENCE ON OUR APPLICATION
        service01.getTargetGroup().configureHealthCheck(
                new HealthCheck.Builder()
                        .path("/actuator/health")
                        .port("8080")
                        .healthyHttpCodes("200")
                        .build()
        );

        /*  SET ON AUTOSCALING RELATED AT THIS SERVICE, SETUP A MINIMAL AND MAX RUNNING INSTANCES GENERATE BY AUTOSCALING*/
        ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2)
                .maxCapacity(4)
                .build());

        //SET A CPU RESOURCE TARGET UTILIZATION AS A TRIGGER TO SCALE UP AND DOWN. DEFINED TIME TO WATCH AS ULTILIZATION BEFORE SCALING.
        scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());

        // ABOVE HOW TO SET AUTOSCALE USING VMEMORY AS A PARAMETER
//        scalableTaskCount.scaleOnMemoryUtilization("Service01AutoScalingMemory", MemoryUtilizationScalingProps.builder()
//                .targetUtilizationPercent(70)
//                .scaleInCooldown(Duration.seconds(60))
//                .scaleOutCooldown(Duration.seconds(60))
//                .build());
    }
}
