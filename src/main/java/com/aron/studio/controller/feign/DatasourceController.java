package com.aron.studio.controller.feign;

import com.aron.studio.data.Response;
import com.aron.studio.data.feign.datasource.CreateDatasourceReq;
import com.aron.studio.data.feign.datasource.DeleteDatasourceReq;
import com.aron.studio.data.feign.datasource.TestConnectionReq;
import com.aron.studio.data.feign.datasource.UpdateDatasourceReq;
import com.aron.studio.data.feign.datasource.vo.DatasourceTypeVO;
import com.aron.studio.data.feign.datasource.vo.DatasourceVO;
import com.aron.studio.feign.DatasourceFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 数据源管理，透传调用，为了此项目引用包干净，此 controller 代理了 studio-engine 的 DatasourceController
 */
@Slf4j
@RestController
@RequestMapping("/api/datasource")
public class DatasourceController {

    @Autowired
    private DatasourceFeignClient datasourceFeignClient;

    @GetMapping("/selectDatasourceType")
    public Response<List<DatasourceTypeVO>> selectDatasourceType(
            @RequestParam(name = "typeCode", required = false) String typeCode) {
        return datasourceFeignClient.selectDatasourceType(typeCode);
    }

    @PostMapping("/createDatasource")
    public Response<String> createDatasource(@RequestBody CreateDatasourceReq req) {
        return datasourceFeignClient.createDatasource(req);
    }

    @PostMapping("/updateDatasource")
    public Response<Integer> updateDatasource(@RequestBody UpdateDatasourceReq req) {
        return datasourceFeignClient.updateDatasource(req);
    }

    @PostMapping("/deleteDatasource")
    public Response<Integer> deleteDatasource(@RequestBody DeleteDatasourceReq req) {
        return datasourceFeignClient.deleteDatasource(req);
    }

    @GetMapping("/selectDatasource")
    public Response<List<DatasourceVO>> selectDatasource() {
        return datasourceFeignClient.selectDatasource();
    }

    @PostMapping("/testConnection")
    public Response<String> testConnection(@RequestBody TestConnectionReq req) {
        return datasourceFeignClient.testConnection(req);
    }


}
