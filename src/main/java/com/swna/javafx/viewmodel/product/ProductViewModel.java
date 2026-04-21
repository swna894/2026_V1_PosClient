package com.swna.javafx.viewmodel.product;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.domain.product.PageResult;
import com.swna.javafx.domain.product.Product;
import com.swna.javafx.repository.product.ProductApiRepository;
import com.swna.javafx.service.product.ProductQueryUseCase;
import com.swna.javafx.viewmodel.BaseViewModel;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

@Component
public class ProductViewModel extends BaseViewModel {

    private final ProductApiRepository repository;
    private final ProductQueryUseCase useCase;

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final StringProperty keyword = new SimpleStringProperty();
    private final IntegerProperty page = new SimpleIntegerProperty(0);
    private final IntegerProperty size = new SimpleIntegerProperty(20);



    public ProductViewModel(ProductApiRepository repository,
                ProductQueryUseCase useCase ) {
        this.repository = repository;
        this.useCase = useCase;
    }

    public ObservableList<Product> getProducts() {
        return products;
    }

    public StringProperty keywordProperty() {
        return keyword;
    }

    public IntegerProperty pageProperty() {
        return page;
    }

    public IntegerProperty sizeProperty() {
        return size;
    }
    
 // ================= ERP 핵심: Paging API =================
    public void load() {

        setLoading(true);

        Task<PageResult<Product>> task = new Task<>() {
            @Override
            protected PageResult<Product> call() {
                return useCase.getProducts(
                        keyword.get(),
                        page.get(),
                        size.get()
                );
            }
        };

        task.setOnSucceeded(e -> {
            PageResult<Product> result = task.getValue();

            products.setAll(result.getContent());

            setLoading(false);
            setError(null);
        });

        task.setOnFailed(e -> {
            setLoading(false);
            setError(task.getException().getMessage());
        });

        runAsync(task);
    }

    public void nextPage() {
        page.set(page.get() + 1);
        load();
    }

    public void prevPage() {
        if (page.get() > 0) {
            page.set(page.get() - 1);
            load();
        }
    }
    // ================= API 호출 =================
    public void loadProducts() {

        setLoading(true);

        Task<List<Product>> task = new Task<>() {
            @Override
            protected List<Product> call() {
                return repository.fetchProducts();
            }
        };

        task.setOnSucceeded(e -> {
            products.setAll(task.getValue());
            setLoading(false);
        });

        task.setOnFailed(e -> {
            setLoading(false);
            setError(task.getException().getMessage());
        });

        runAsync(task);
    }

    // ================= 검색 (클라이언트 필터 or API 확장) =================
    public void search() {

        setLoading(true);

        Task<List<Product>> task = new Task<>() {
            @Override
            protected List<Product> call() {

                List<Product> all = repository.fetchProducts();

                if (keyword.get() == null || keyword.get().isBlank()) {
                    return all;
                }

                String kw = keyword.get().toLowerCase();

                return all.stream()
                        .filter(p -> p.getName().toLowerCase().contains(kw))
                        .toList();
            }
        };

        task.setOnSucceeded(e -> {
            products.setAll(task.getValue());
            setLoading(false);
        });

        task.setOnFailed(e -> {
            setLoading(false);
            setError(task.getException().getMessage());
        });

        runAsync(task);
    }
}