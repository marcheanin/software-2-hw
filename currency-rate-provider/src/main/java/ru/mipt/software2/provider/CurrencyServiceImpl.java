package ru.mipt.software2.provider;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.mipt.software2.api.currency.CurrencyServiceGrpc;
import ru.mipt.software2.api.currency.GetRateRequest;
import ru.mipt.software2.api.currency.GetRateResponse;

@Service
public class CurrencyServiceImpl extends CurrencyServiceGrpc.CurrencyServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(CurrencyServiceImpl.class);

    private final RateService rateService;

    public CurrencyServiceImpl(RateService rateService) {
        this.rateService = rateService;
    }

    @Override
    public void getRate(GetRateRequest request, StreamObserver<GetRateResponse> responseObserver) {
        if (log.isDebugEnabled()) {
            log.debug("gRPC server request payload: {}", request);
        }

        double rate = rateService.getCurrentRate();

        GetRateResponse response = GetRateResponse.newBuilder()
                .setUsdrub(rate)
                .build();

        if (log.isDebugEnabled()) {
            log.debug("gRPC server response payload: {}", response);
        }
        log.info("GetRate usdrub={}", rate);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

