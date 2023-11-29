package com.example.web.service.product;

import com.example.web.dto.product.UserProductBuyDto;
import com.example.web.dto.product.ProductEditDto;
import com.example.web.dto.product.ProductInfoDto;
import com.example.web.dto.product.UserProductLogDto;
import com.example.web.dto.product.UserProductInfoDto;
import com.example.web.jpa.entity.product.Product;
import com.example.web.jpa.entity.product.UserProduct;
import com.example.web.jpa.entity.product.UserProductLog;
import com.example.web.jpa.entity.product.id.UserProductId;
import com.example.web.jpa.entity.user.UserInfo;
import com.example.web.jpa.repository.product.ProductRepository;
import com.example.web.jpa.repository.product.UserProductLogRepository;
import com.example.web.jpa.repository.product.UserProductLogRepositorySupport;
import com.example.web.jpa.repository.product.UserProductRepository;
import com.example.web.model.enums.ProductType;
import com.example.web.model.exception.CustomErrorException;
import com.example.web.service.ServiceBase;
import com.example.web.service.user.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class ProductService extends ServiceBase {

  private final ProductRepository productRepository;
  private final UserProductRepository userProductRepository;
  private final UserProductLogRepository userProductLogRepository;
  private final UserProductLogRepositorySupport userProductLogRepositorySupport;
  private final UserService userService;

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

  public ProductInfoDto.Response getProductInfo() {

    return ProductInfoDto.Response.builder()
        .products(productRepository.findAll())
        .build();
  }

  public UserProductInfoDto.Response getUserProductInfo() {

    List<UserProduct> userProducts = userProductRepository
        .findByUserIndex(getUserIndex());

    return UserProductInfoDto.Response
        .builder()
        .userProducts(userProducts)
        .build();
  }

  private Product getNewProduct(ProductType productType, String productName, int price,
      int quantity) {
    return Product.builder()
        .productType(productType)
        .productName(productName)
        .price(price)
        .quantity(quantity)
        .build();
  }

  @Transactional
  public ProductEditDto.Response editProduct(ProductEditDto.Request request) {
    // 1. 수정할 정보 획득
    Product product = getProduct(request);
    // 2. 정보 저장
    saveProductInfo(product);

    return ProductEditDto.Response.builder()
        .product(product)
        .build();
  }

  /**
   * 수정할 상품 정보 획득 요청시 수정할 상품 이름이 없으면 새롭게 등록한다.
   *
   * @param request
   * @return 수정할 상품 정보
   */
  private Product getProduct(ProductEditDto.Request request) {
    String productName = request.getProductName();

    return productRepository.findByProductName(productName)
        .orElseGet(() -> getNewProduct(
            request.getProductType(),
            productName,
            request.getPrice(),
            request.getQuantity()));
  }

  private void saveProductInfo(Product product) {
    productRepository.save(product);
  }

  @Transactional
  public UserProductBuyDto.Response buyUserProduct(UserProductBuyDto.Request request) {
    // 1. dto 생성
    UserProductBuyDto.Dto dto = getDto(request);
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

    return UserProductBuyDto.Response.builder()
        .userMoney(dto.getUserInfo().getMoney())
        .userProduct(dto.getUserProduct())
        .build();
  }

  private UserProductBuyDto.Dto getDto(UserProductBuyDto.Request request) {
    // 1. 상품 정보 조회
    UserInfo userInfo = userService.getUserInfoOrElseThrow(getUserIndex());
    // 2. 상품 기획 데이터 정보 조회
    Product product = getProductOrElseThrow(request.getProductIndex());
    // 3. 유저 상품 정보 조회
    UserProduct userProduct = getUserProduct(product.getProductIndex(), userInfo.getUserIndex(),
        product);

    return UserProductBuyDto.Dto.builder()
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

  private UserProduct getUserProduct(int productIndex, long userIndex, Product product) {
    UserProductId userProductId = UserProductId.builder()
        .product(productIndex)
        .userIndex(userIndex)
        .build();

    return userProductRepository.findById(userProductId)
        .orElseGet(() -> UserProduct.builder()
            .userIndex(userIndex)
            .product(product)
            .updatedAt(OffsetDateTime.now())
            .build());
  }

  private void checkProductCount(UserProductBuyDto.Dto dto) {
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
  private void checkUserMoney(UserProductBuyDto.Dto dto) {
    // 필요한 돈 = 상품 가격 * 구매 상품 개수
    long needMoney = dto.getProduct().getPrice() * dto.getRequest().getProductCount();

    userService.checkEnoughMoney(needMoney, dto.getUserInfo());
  }

  private void minusProductCount(UserProductBuyDto.Dto dto) {
    Product product = dto.getProduct();
    product.addProductQuantity(-1 * dto.getRequest().getProductCount());
  }

  private void minusUserMoney(UserProductBuyDto.Dto dto) {
    // 필요한 돈 = 상품 가격 * 구매 상품 개수
    long needMoney = dto.getProduct().getPrice() * dto.getRequest().getProductCount();

    UserInfo userInfo = dto.getUserInfo();
    userInfo.addMoney(-1 * needMoney);
  }

  private void addUserProduct(UserProductBuyDto.Dto dto) {
    UserProduct userProduct = dto.getUserProduct();
    userProduct.addProductCount(dto.getRequest().getProductCount());
  }

  private void setProductBuyLog(UserProductBuyDto.Dto dto) {
    UserProductBuyDto.Request request = dto.getRequest();
    int afterProductQuantity = dto.getProduct().getQuantity();
    int beforeProductQuantity = afterProductQuantity - request.getProductCount();

    UserProductLog userProductLog = UserProductLog.builder()
        .userIndex(dto.getUserInfo().getUserIndex())
        .productIndex(request.getProductIndex())
        .afterProductCount(afterProductQuantity)
        .beforeProductCount(beforeProductQuantity)
        .build();

    dto.setUserProductLog(userProductLog);
  }

  private void saveProductBuy(UserProductBuyDto.Dto dto) {
    userService.saveUserInfo(dto.getUserInfo());
    productRepository.save(dto.getProduct());
    userProductRepository.save(dto.getUserProduct());
    userProductLogRepository.save(dto.getUserProductLog());
  }

  public UserProductLogDto.Response getUserProductLog(UserProductLogDto.Dto dto) {
    List<UserProductLog> userProductLogs = userProductLogRepositorySupport
        .getUserProductLogs(getUserIndex(), dto.getProductIndex(),
            dto.getStartTime(), dto.getEndTime());

    return UserProductLogDto.Response.builder()
        .userProductLogs(userProductLogs)
        .build();
  }
}
