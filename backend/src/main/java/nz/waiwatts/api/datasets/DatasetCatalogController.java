package nz.waiwatts.api.datasets;

import nz.waiwatts.domain.datasets.DatasetRelease;
import nz.waiwatts.domain.datasets.DatasetSource;
import nz.waiwatts.persistence.repositories.DatasetReleaseRepository;
import nz.waiwatts.persistence.repositories.DatasetSourceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/datasets")
public class DatasetCatalogController {

    private final nz.waiwatts.service.datasets.DatasetCatalogService datasetSourceService;

    public DatasetCatalogController(nz.waiwatts.service.datasets.DatasetCatalogService datasetSourceService) {
        this.datasetSourceService = datasetSourceService;
    }

    @GetMapping("/sources")
    public List<nz.waiwatts.api.datasets.dto.DatasetSourceDto> listSources() {
        return datasetSourceService.findAllSources().stream()
                .map(nz.waiwatts.api.datasets.dto.DatasetSourceDto::from)
                .toList();
    }

    @GetMapping("/sources/{id}/releases")
    public ResponseEntity<List<nz.waiwatts.api.datasets.dto.DatasetReleaseDto>> listReleasesBySource(@PathVariable("id") UUID id) {
        return datasetSourceService.findSourceById(id)
                .map(src -> ResponseEntity.ok(
                        datasetSourceService.findReleasesBySourceId(id).stream().map(nz.waiwatts.api.datasets.dto.DatasetReleaseDto::from).toList()
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
