package com.aron.studio.feign;

import com.aron.studio.data.Response;
import com.aron.studio.data.feign.datasource.CreateDatasourceReq;
import com.aron.studio.data.feign.datasource.DeleteDatasourceReq;
import com.aron.studio.data.feign.datasource.TestConnectionReq;
import com.aron.studio.data.feign.datasource.UpdateDatasourceReq;
import com.aron.studio.data.feign.datasource.vo.DatasourceTypeVO;
import com.aron.studio.data.feign.datasource.vo.DatasourceVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "studio-engine", url = "${feign.client.url.studio-engine}")
public interface DatasourceFeignClient {

    // feign 返回 LinkedHashmap
    @GetMapping("/api/datasource/selectDatasourceType")
    Response<List<DatasourceTypeVO>> selectDatasourceType(
            @RequestParam(name = "typeCode", required = false) String typeCode);

    @PostMapping("/api/datasource/createDatasource")
    Response<String> createDatasource(@RequestBody CreateDatasourceReq req);

    @PostMapping("/api/datasource/updateDatasource")
    Response<Integer> updateDatasource(@RequestBody UpdateDatasourceReq req);

    @PostMapping("/api/datasource/deleteDatasource")
    Response<Integer> deleteDatasource(@RequestBody DeleteDatasourceReq req);

    @GetMapping("/api/datasource/selectDatasource")
    Response<List<DatasourceVO>> selectDatasource();

    @PostMapping("/api/datasource/testConnection")
    Response<String> testConnection(@RequestBody TestConnectionReq req);
}
