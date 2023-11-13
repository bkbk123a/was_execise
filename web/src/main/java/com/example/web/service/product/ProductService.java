package com.example.web.service.product;

import com.example.web.dto.product.ProductBuyDto;
import com.example.web.dto.product.ProductInfoDto;
import com.example.web.dto.product.UserProductInfoDto;
import com.example.web.jpa.entity.product.Product;
import com.example.web.jpa.entity.product.UserProduct;
import com.example.web.jpa.entity.product.UserProductLog;
import com.example.web.jpa.entity.product.id.UserProductId;
import com.example.web.jpa.entity.user.UserInfo;
import com.example.web.jpa.repository.item.ProductRepository;
import com.example.web.jpa.repository.item.UserProductLogRepository;
import com.example.web.jpa.repository.item.UserProductRepository;
import com.example.web.model.enums.ProductType;
import com.example.web.model.exception.CustomErrorException;
import com.example.web.service.ServiceBase;
import com.example.web.service.user.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService extends ServiceBase {

  private final ProductRepository productRepository;
  private final UserProductRepository userProductRepository;
  private final UserService userService;
  private final UserProductLogRepository userProductLogRepository;

  @PostConstruct
  private void init() {
    List<Product> staticProducts = new ArrayList<>();

    staticProducts.add(getNewProduct(ProductType.PANTS, "청바지1", 100, 999));
    staticProducts.add(getNewProduct(ProductType.PANTS, "청바지2", 200, 999));
    staticProducts.add(getNewProduct(ProductType.PANTS, "청바지3", 300, 999));
    staticProducts.add(getNewProduct(ProductType.PANTS, "청바지4", 400, 999));
    staticProducts.add(getNewProduct(ProductType.TOP, "상의1", 100, 999));
    staticProducts.add(getNewProduct(ProductType.TOP, "상의2", 200, 999));
    staticProducts.add(getNewProduct(ProductType.TOP, "상의3", 300, 999));
    staticProducts.add(getNewProduct(ProductType.TOP, "상의4", 400, 999));
    staticProducts.add(getNewProduct(ProductType.TOP, "신발1", 100, 999));
    staticProducts.add(getNewProduct(ProductType.TOP, "신발2", 200, 999));
    staticProducts.add(getNewProduct(ProductType.TOP, "신발3", 300, 999));
    staticProducts.add(getNewProduct(ProductType.TOP, "신발4", 400, 999));

    productRepository.saveAll(staticProducts);
  }

  public ProductInfoDto.Response getProductssInfo() {

    return ProductInfoDto.Response.builder()
        .products(productRepository.findAll())
        .build();
  }

  public UserProductInfoDto.Response getUserProductsInfo() {

    List<UserProduct> userProducts = userProductRepository
        .findByUserIndex(getUserIndex());

    return UserProductInfoDto.Response
        .builder()
        .userProducts(userProducts)
        .build();
  }

  private Product getNewProduct( ProductType productType, String productName, int price, int quantity) {
    return Product.builder()
        .productType(productType)
        .productName(productName)
        .price(price)
        .quantity(quantity)
        .build();
  }

  @Transactional
  public ProductBuyDto.Response buyProduct(ProductBuyDto.Request request) {
    // 1. dto 생성
    ProductBuyDto.Dto dto = getDto(request);
    // 2. 유저가 구매할 수 있는 상품의 남은량 확인
    checkProductCount(dto);
    // 3. 유저가 구매하는데 필요한 돈 충분한지 확인
    checkUserMoney(dto);
    // 4. 상품 기획데이터 개수 차감
    minusProductCount(dto);
    // 5. 유저의 재화 차감
    minusUserMoney(dto);
    // 6. 유저의 상품 개수 증가
    addUserProduct(dto);
    // 7. 유저의 구매 이력 로그
    setProductBuyLog(dto);
    // 8. DB 반영
    saveProductBuy(dto);

    return ProductBuyDto.Response.builder()
        .userMoney(dto.getUserInfo().getMoney())
        .userProduct(dto.getUserProduct())
        .build();
  }

  private ProductBuyDto.Dto getDto(ProductBuyDto.Request request) {
    // 1. 상품 정보 조회
    UserInfo userInfo = userService.getUserInfoOrElseThrow(getUserIndex());
    // 2. 상품 기획 데이터 정보 조회
    Product product = getProductOrElseThrow(request.getProductIndex());
    // 3. 유저 상품 정보 조회
    UserProduct userProduct = getUserProduct(product.getProductIndex(), userInfo.getUserIndex());

    return ProductBuyDto.Dto.builder()
        .userInfo(userInfo)
        .product(product)
        .userProduct(userProduct)
        .request(request)
        .build();
  }

  private Product getProductOrElseThrow(int itemIndex) {
    return productRepository.findById(itemIndex)
        .orElseThrow(() -> CustomErrorException.builder().resultValue(10100).build());
  }

  private UserProduct getUserProduct(int productIndex, long userIndex) {
    UserProductId userProductId = UserProductId.builder()
        .productIndex(productIndex)
        .userIndex(userIndex)
        .build();

    return userProductRepository.findById(userProductId)
        .orElseGet(() -> UserProduct.builder()
            .userIndex(userIndex)
            .productIndex(productIndex)
            .updatedAt(OffsetDateTime.now())
            .build());
  }

  private void checkProductCount(ProductBuyDto.Dto dto) {
    int needItemCount = dto.getRequest().getProductCount();
    int remainItemCount = dto.getProduct().getQuantity();

    if (remainItemCount < needItemCount) {
      throw CustomErrorException.builder().resultValue(10101).build();
    }
  }

  /**
   * 상품 구매에 필요한 돈 충분한지 확인
   *
   * @param dto
   */
  private void checkUserMoney(ProductBuyDto.Dto dto) {
    // 필요한 돈 = 상품 가격 * 구매 상품 개수
    long needMoney = dto.getProduct().getPrice() * dto.getRequest().getProductCount();

    userService.checkEnoughMoney(needMoney, dto.getUserInfo());
  }

  private void minusProductCount(ProductBuyDto.Dto dto) {
    Product product = dto.getProduct();
    product.addProductQuantity(-1 * dto.getRequest().getProductCount());
  }

  private void minusUserMoney(ProductBuyDto.Dto dto) {
    // 필요한 돈 = 상품 가격 * 구매 상품 개수
    long needMoney = dto.getProduct().getPrice() * dto.getRequest().getProductCount();

    UserInfo userInfo = dto.getUserInfo();
    userInfo.addMoney(-1 * needMoney);
  }

  private void addUserProduct(ProductBuyDto.Dto dto) {
    UserProduct userProduct = dto.getUserProduct();
    userProduct.addProductCount(dto.getRequest().getProductCount());
  }

  private void setProductBuyLog(ProductBuyDto.Dto dto) {
    ProductBuyDto.Request request = dto.getRequest();
    int afterProductQuantity = dto.getProduct().getQuantity();
    int beforeProductQuantity = afterProductQuantity - request.getProductCount();

    UserProductLog userProductLog = UserProductLog.builder()
        .userIndex(dto.getUserInfo().getUserIndex())
        .itemIndex(request.getProductIndex())
        .afterProductCount(afterProductQuantity)
        .beforeProductCount(beforeProductQuantity)
        .build();

    dto.setUserProductLog(userProductLog);
  }

  private void saveProductBuy(ProductBuyDto.Dto dto) {
    userService.saveUserInfo(dto.getUserInfo());
    productRepository.save(dto.getProduct());
    userProductRepository.save(dto.getUserProduct());
    userProductLogRepository.save(dto.getUserProductLog());
  }
}